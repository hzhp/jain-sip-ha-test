package com.cmcc.ics.rcs2.siphademo;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.DialogState;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.InvalidArgumentException;
import javax.sip.ListeningPoint;
import javax.sip.PeerUnavailableException;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipException;
import javax.sip.SipFactory;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.Transaction;
import javax.sip.TransactionState;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.ExpiresHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.RecordRouteHeader;
import javax.sip.header.RouteHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;

import gov.nist.javax.sip.stack.SIPServerTransaction;
import gov.nist.javax.sip.stack.SIPServerTransactionImpl;
import org.apache.log4j.BasicConfigurator;
import org.mobicents.ha.javax.sip.ClusteredSipStack;
import org.mobicents.ha.javax.sip.ReplicationStrategy;
import org.mobicents.ha.javax.sip.SipStackImpl;

/**
 * @Type JainSipHaDemo
 * @Desc JainSipHaDemo
 * @author hpai
 * @date 2018年03月08日 09:22
 * @version
 */
public class JainSipHaDemo {
    private static final String myAddress = "192.168.100.111";

    class Shootme implements SipListener {

        private SipStack sipStack;
        private AddressFactory addressFactory;
        private MessageFactory messageFactory;
        private HeaderFactory headerFactory;

        private String stackName;

        private String cacheName;

        public int myPort = 5070;

        protected ServerTransaction inviteTid;

        private Response okResponse;

        private Request inviteRequest;

        protected Dialog dialog;

        public boolean immediateAnswer = true;

        private SipProvider sipProvider;

        private boolean firstTxComplete = false;
        private boolean byeComplete = false;
        private boolean dialogRemoved = false;

        private ReplicationStrategy replicationStrategy;

        int countSipMsgMessage = 0;
        int countSipMsgSubscribe = 0;

        public Shootme(String stackName, String cacheName, int myPort, boolean immediateAnswer) {
            this.stackName = stackName;
            this.cacheName = cacheName;
            this.myPort = myPort;
            this.immediateAnswer = immediateAnswer;
            this.replicationStrategy = ReplicationStrategy.ConfirmedDialog;
            System.setProperty("java.net.preferIPv4Stack", "true");
        }

        public Shootme(String stackName, String cacheName, int myPort,
                       boolean immediateAnswer, ReplicationStrategy rs) {
            this.stackName = stackName;
            this.cacheName = cacheName;
            this.myPort = myPort;
            this.immediateAnswer = immediateAnswer;
            this.replicationStrategy = rs;
            System.setProperty("java.net.preferIPv4Stack", "true");
        }

        class ByeTask extends TimerTask {
            public void run() {
                try {
                    Request byeRequest = dialog.createRequest(Request.BYE);
                    ClientTransaction ct = sipProvider
                            .getNewClientTransaction(byeRequest);
                    dialog.sendRequest(ct);

                } catch (Exception ex) {
                    ex.printStackTrace();
                    System.out.println("Shootme: unexpected exception sending bye: " + ex.toString());
                }
            }

        }

        class MyTimerTask extends TimerTask {
            Shootme shootme;

            public MyTimerTask(Shootme shootme) {
                this.shootme = shootme;

            }

            public void run() {
                shootme.sendInviteOK();
            }

        }

        public SipStack getSipStack() {
            return sipStack;
        }

        protected static final String usageString = "java "
                + "examples.shootist.Shootist \n"
                + ">>>> is your class path set to the root?";

        public void processRequest(RequestEvent requestEvent) {
            Request request = requestEvent.getRequest();
            ServerTransaction serverTransactionId = requestEvent
                    .getServerTransaction();

			/*System.out.println("\n\nShootme: request " + request.getMethod()
					+ " received at " + sipStack.getStackName()
					+ " with server transaction id " + serverTransactionId);
			*/
            //System.out.println("DialogId: " + requestEvent.getDialog().getDialogId());

            if (request.getMethod().equals(Request.INVITE)) {
                processInvite(requestEvent, serverTransactionId);
            } else if (request.getMethod().equals(Request.ACK)) {
                processAck(requestEvent, serverTransactionId);
            } else if (request.getMethod().equals(Request.BYE)) {
                processBye(requestEvent, serverTransactionId);
            } else if (request.getMethod().equals(Request.CANCEL)) {
                processCancel(requestEvent, serverTransactionId);
            } else if (request.getMethod().equals(Request.MESSAGE)) {
                processMessage(requestEvent, serverTransactionId);
            } else if (request.getMethod().equals(Request.SUBSCRIBE)) {
                processSubscribe(requestEvent, serverTransactionId);
            } else {
                try {
                    serverTransactionId.sendResponse(messageFactory
                            .createResponse(202, request));

                    // send one back
                    SipProvider prov = (SipProvider) requestEvent.getSource();
                    Request refer = requestEvent.getDialog().createRequest(
                            "REFER");
                    requestEvent.getDialog().sendRequest(
                            prov.getNewClientTransaction(refer));

                } catch (SipException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (InvalidArgumentException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (ParseException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

        }

        public void processResponse(ResponseEvent responseEvent) {
            Dialog dialog = responseEvent.getDialog();
            CSeqHeader cSeqHeader = (CSeqHeader) responseEvent.getResponse()
                    .getHeader(CSeqHeader.NAME);
            try {
                if (responseEvent.getResponse().getStatusCode() >= 200
                        && cSeqHeader.getMethod().equalsIgnoreCase(
                        Request.INVITE)) {
                    Request ackRequest = dialog.createAck(cSeqHeader
                            .getSeqNumber());
                    System.out.println("Shootme: sending ACK");
                    dialog.sendAck(ackRequest);

                } else if (responseEvent.getResponse().getStatusCode() == 200
                        && cSeqHeader.getMethod().equalsIgnoreCase(
                        Request.BYE)) {
                    byeComplete = true;
                }
            } catch (SipException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InvalidArgumentException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
        // <hpai
        public void processMessage(RequestEvent requestEvent, ServerTransaction serverTransactionId) {
            countSipMsgMessage = countSipMsgMessage + 1;
            System.out.println("%%%receive SIP MESSAGE count: " + countSipMsgMessage);
            try {
                Response response = messageFactory.createResponse(Response.ACCEPTED, requestEvent.getRequest());

                ToHeader toHeader = (ToHeader) response.getHeader(ToHeader.NAME);
                toHeader.setTag("654321");

                if (serverTransactionId == null) {
                    System.out.println("SIP Rquest's Transaction is null! And get a new server Transaction now!");
                    serverTransactionId = sipProvider.getNewServerTransaction(requestEvent.getRequest());
                }
                else {
                    System.out.println("Transaction exist and transaction Id: "+ ((SIPServerTransactionImpl) serverTransactionId).getTransactionId());
                }

                SIPServerTransactionImpl sipServerTransaction = (SIPServerTransactionImpl) serverTransactionId;

                System.out.println("Before Response Transaction id: " + sipServerTransaction.getTransactionId());
                sipServerTransaction.setApplicationData("1234567890abcdefg");

                //serverTransactionId.sendResponse(response);

                System.out.println("Transaction id: " + sipServerTransaction.getTransactionId());
                sipServerTransaction.setApplicationData("1234567890abcdefg");

            } catch (Exception e) {
                System.out.println(e.toString());
            }

        }

        public void processSubscribe(RequestEvent requestEvent, ServerTransaction serverTransactionId) {
            countSipMsgSubscribe = countSipMsgSubscribe + 1;
            System.out.println("%%%receive SIP SUBSCRIBE count: " + countSipMsgSubscribe);
            try {
                Response response = messageFactory.createResponse(Response.ACCEPTED, requestEvent.getRequest());

                ToHeader toHeader = (ToHeader) response.getHeader(ToHeader.NAME);
                toHeader.setTag("654321");
                // Expires Header
                ExpiresHeader expiresHeader = headerFactory.createExpiresHeader(3600);
                response.addHeader(expiresHeader);

                if (serverTransactionId == null) {
                    System.out.println("SIP Rquest's Transaction is null! And get a new server Transaction now!");
                    serverTransactionId = sipProvider.getNewServerTransaction(requestEvent.getRequest());
                }
                else {
                    System.out.println("Transaction exist and transaction Id: "+ ((SIPServerTransactionImpl) serverTransactionId).getTransactionId());
                }

                Dialog tmpdialog = requestEvent.getDialog();
                if (tmpdialog != null) {
                    SessionCache sessionCache = (SessionCache) tmpdialog.getApplicationData();
                    System.out.println("get session cache in ApplicationData: " + sessionCache.toString());

                } else {
                    SessionCache sessionCache = new SessionCache();
                    sessionCache.setSessionId(UUID.randomUUID().toString());
                    sessionCache.setCurrentState(2);
                    sessionCache.setContents("my content with 123 abc");
                    tmpdialog = serverTransactionId.getDialog();

                    sessionCache.setTransactionId(((SIPServerTransaction)serverTransactionId).getTransactionId());
                    tmpdialog.setApplicationData(sessionCache);
                    System.out.println("dialog id: " + tmpdialog.getDialogId());
                    System.out.println("put session cache in ApplictionData: " + sessionCache.toString());


                }

                serverTransactionId.sendResponse(response);
                System.out.println("dialog id: " + tmpdialog.getDialogId());

            } catch (Exception e) {
                System.out.println(e.toString());
            }
        }
        // hpai>

        /**
         * Process the ACK request. Send the bye and complete the call flow.
         */
        public void processAck(RequestEvent requestEvent,
                               ServerTransaction serverTransaction) {
            try {
                Dialog dialog = serverTransaction.getDialog();
                System.out.println("Shootme: got an ACK! ");
                System.out.println("Shootme: dialog State = " + dialog.getState());
                firstTxComplete = true;

            } catch (Exception ex) {
                ex.printStackTrace();
            }

        }

        /**
         * Process the invite request.
         */
        public void processInvite(RequestEvent requestEvent,
                                  ServerTransaction serverTransaction) {
            SipProvider sipProvider = (SipProvider) requestEvent.getSource();
            Request request = requestEvent.getRequest();
            // <hpai
            System.out.print("=====>request message: " + request.toString());
            SessionCache sessionCache = new SessionCache();
            sessionCache.setSessionId(UUID.randomUUID().toString());
            sessionCache.setCurrentState(2);
            sessionCache.setContents("my content with 123 abc");
            // hpai>
            try {
                System.out.println("Shootme: got an Invite sending Trying");
                Response response = messageFactory.createResponse(Response.RINGING, request);
                ToHeader toHeader = (ToHeader) response.getHeader(ToHeader.NAME);
                toHeader.setTag("4321"); // Application is supposed to set.

                ServerTransaction st = requestEvent.getServerTransaction();

                if (st == null) {
                    st = sipProvider.getNewServerTransaction(request);
                    System.out.println("getNewServerTransaction");
                }
                dialog = st.getDialog();
                inviteTid = st;
                inviteRequest = request;
                //dialog.setApplicationData(((SIPServerTransaction)st).getTransactionId());
                // <hpai
                sessionCache.setTransactionId(((SIPServerTransaction)st).getTransactionId());
                dialog.setApplicationData(sessionCache);
                // hpai>

                st.sendResponse(response);

                Thread.sleep(1000);

                if (this.immediateAnswer) {
                    this.okResponse = messageFactory.createResponse(Response.OK,
                            request);
                    Address address = addressFactory.createAddress("Shootme <sip:"
                            + myAddress + ":" + myPort + ">");
                    ContactHeader contactHeader = headerFactory
                            .createContactHeader(address);
                    response.addHeader(contactHeader);
                    toHeader = (ToHeader) okResponse.getHeader(ToHeader.NAME);
                    toHeader.setTag("4321"); // Application is supposed to set.
                    okResponse.addHeader(contactHeader);

                    sendInviteOK();
                    System.out.println("DialogId#### " + dialog.getDialogId());
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(0);
            }
        }

        private void sendInviteOK() {
            try {
                if (inviteTid.getState() != TransactionState.COMPLETED) {
                    System.out.println("Shootme: dialog state before 200: "
                            + inviteTid.getDialog().getState());
                    inviteTid.sendResponse(okResponse);
                    System.out.println("Shootme: dialog state after 200: "
                            + inviteTid.getDialog().getState());
                }
            } catch (SipException ex) {
                ex.printStackTrace();
            } catch (InvalidArgumentException ex) {
                ex.printStackTrace();
            }
        }

        /**
         * Process the bye request.
         */
        public void processBye(RequestEvent requestEvent,
                               ServerTransaction serverTransactionId) {
            Request request = requestEvent.getRequest();
            // <hpai
            Dialog tmpDialog = requestEvent.getDialog();
            System.out.println("BYE's DialogId = " + tmpDialog.getDialogId());
            SessionCache tmpSessionCache = (SessionCache) requestEvent.getDialog().getApplicationData();
            System.out.println("SessionCache.getSessionId: " + tmpSessionCache.getSessionId());
            System.out.println("SessionCache.getTransactionId: " + tmpSessionCache.getTransactionId());
            System.out.println("SessionCache.getCurrentState: " + tmpSessionCache.getCurrentState());
            System.out.println("SessionCache.getContents: " + tmpSessionCache.getContents());
            // hpai>
            try {
                System.out.println("Shootme:  got a bye sending OK.");
                Response response = messageFactory.createResponse(200, request);
                serverTransactionId.sendResponse(response);
                System.out.println("Shootme: dialog State is "
                        + serverTransactionId.getDialog().getState());
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(0);

            }
        }

        public void processCancel(RequestEvent requestEvent,
                                  ServerTransaction serverTransactionId) {
            Request request = requestEvent.getRequest();
            try {
                System.out.println("Shootme:  got a cancel.");
                if (serverTransactionId == null) {
                    System.out.println("Shootme:  null tid.");
                    return;
                }
                Response response = messageFactory.createResponse(200, request);
                serverTransactionId.sendResponse(response);
                if (dialog.getState() != DialogState.CONFIRMED) {
                    response = messageFactory.createResponse(
                            Response.REQUEST_TERMINATED, inviteRequest);
                    inviteTid.sendResponse(response);
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(0);

            }
        }

        public void processTimeout(javax.sip.TimeoutEvent timeoutEvent) {
            Transaction transaction;
            if (timeoutEvent.isServerTransaction()) {
                transaction = timeoutEvent.getServerTransaction();
            } else {
                transaction = timeoutEvent.getClientTransaction();
            }
            System.out.println("Shootme: dialog = " + transaction.getDialog());
            System.out.println("Shootme: dialogState = "
                    + transaction.getDialog().getState());
            System.out.println("Shootme: transaction Timed out");
        }

        public void init() {
            SipFactory sipFactory = null;
            sipStack = null;
            sipFactory = SipFactory.getInstance();
            sipFactory.setPathName("org.mobicents.ha");
            Properties properties = new Properties();
            properties.setProperty("javax.sip.STACK_NAME", stackName);
            // You need 16 for logging traces. 32 for debug + traces.
            // Your code will limp at 32 but it is best for debugging.
            properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "32");
            properties.setProperty("gov.nist.javax.sip.DEBUG_LOG", "logs/"
                    + stackName + "debug.txt");
            properties.setProperty("gov.nist.javax.sip.SERVER_LOG", "logs/"
                    + stackName + "log.xml");
            properties.setProperty("org.mobicents.ha.javax.sip.REPLICATION_STRATEGY",
                    replicationStrategy.toString());
            properties.setProperty(
                    "org.mobicents.ha.javax.sip.CACHE_CLASS_NAME",
                    "org.mobicents.ha.javax.sip.cache.infinispan.InfinispanCache");
            properties.setProperty("org.mobicents.ha.javax.sip.REPLICATE_APPLICATION_DATA", "true");

            try {
                // Create SipStack object
                sipStack = sipFactory.createSipStack(properties);

            } catch (PeerUnavailableException e) {
                // could not find
                // gov.nist.jain.protocol.ip.sip.SipStackImpl
                // in the classpath
                e.printStackTrace();
                System.err.println(e.getMessage());
                if (e.getCause() != null)
                    e.getCause().printStackTrace();
                System.exit(0);
            }

            try {
                headerFactory = sipFactory.createHeaderFactory();
                addressFactory = sipFactory.createAddressFactory();
                messageFactory = sipFactory.createMessageFactory();
                ListeningPoint lp = sipStack.createListeningPoint(myAddress,
                        myPort, ListeningPoint.UDP);

                sipProvider = sipStack.createSipProvider(lp);
                sipProvider.addSipListener(this);
                sipStack.start();

            } catch (Exception ex) {
                ex.printStackTrace();
                System.out.println("Shootme: unexpected exception");
            }
        }

        public void processIOException(IOExceptionEvent exceptionEvent) {
            System.out.println("IOException");

        }

        public void processTransactionTerminated(
                TransactionTerminatedEvent transactionTerminatedEvent) {
            if (transactionTerminatedEvent.isServerTransaction())
                System.out.println("Shootme: transaction terminated -> "
                        + transactionTerminatedEvent.getServerTransaction().getBranchId());
            else
                System.out.println("Shootme: transaction terminated -> "
                        + transactionTerminatedEvent.getClientTransaction().getBranchId());
        }

        public void processDialogTerminated(
                DialogTerminatedEvent dialogTerminatedEvent) {
            dialogRemoved = true;
            System.out.println("Shootme: dialog terminated -> "
                    + dialogTerminatedEvent.getDialog().getDialogId());
        }

        public void stop() {
            sipStack.stop();
        }

        public void checkState() {
            if (firstTxComplete)
                System.out.println("Shootme: first transaction completed. State OK.");
            else
                System.out.println("firstTxComplete " + firstTxComplete);
        }

        public void checkDialogRemoved() {
            if (!dialogRemoved)
                System.out.println("dialog not removed");
        }

        public void checkByeState() {
            if (byeComplete)
                System.out.println("Shootme: BYE completed. State OK.");
            else
                System.out.println("No BYE response received, byeComplete=" + byeComplete);
        }

        public void recoverDialog(String id) {
            dialog = ((SipStackImpl)sipStack).getDialog(id);
        }

        public void sendBye() {
            System.out.println("Shootme: sending bye");
            new Timer().schedule(new ByeTask(), 0);
        }

        public void send200Invite() {
            System.out.println("Shootme: sending 200 OK");
            String txId = (String)dialog.getApplicationData();
            inviteTid = (ServerTransaction)((ClusteredSipStack)sipStack).findTransaction(txId, true);
            try {
                okResponse = messageFactory.createResponse(Response.OK, inviteTid.getRequest());
                Address address = addressFactory.createAddress("Shootme <sip:"+ myAddress + ":" + myPort + ">");
                ContactHeader contactHeader = headerFactory
                        .createContactHeader(address);
                okResponse.addHeader(contactHeader);
                ToHeader toHeader = (ToHeader) okResponse.getHeader(ToHeader.NAME);
                toHeader.setTag("4321"); // Application is supposed to set.
                okResponse.addHeader(contactHeader);

                sendInviteOK();

            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    class Shootist implements SipListener {

        private SipProvider sipProvider;

        private SipStack sipStack;
        private AddressFactory addressFactory;
        private MessageFactory messageFactory;
        private HeaderFactory headerFactory;

        private ContactHeader contactHeader;

        private ListeningPoint udpListeningPoint;

        private ClientTransaction inviteTid;

        private Dialog dialog;

        private String recordRouteAddr;

        private boolean byeTaskRunning;

        public int myPort = 5050;

        private boolean okToByeReceived;
        // Save the created ACK request, to respond to retransmitted 2xx
        private Request ackRequest;

        private String stackName;

        class ByeTask extends TimerTask {

            public void run() {
                try {
                    if (dialog != null && dialog.getState() != DialogState.TERMINATED) {
                        Request byeRequest = dialog.createRequest(Request.BYE);
                        if (byeRequest.getHeader(RouteHeader.NAME) != null) {
                            byeRequest.removeHeader(RouteHeader.NAME);
                        }
                        // <hpai
                        String toUser = "LittleGuy";
                        SipURI requestURI = addressFactory.createSipURI(toUser,
                                "192.168.100.101:5070");
                        byeRequest.setRequestURI(requestURI);
                        // hpai>
                        ClientTransaction ct = sipProvider
                                .getNewClientTransaction(byeRequest);
                        // <hpai
                        System.out.println(">>>>>>> BYE Message: " + byeRequest.toString());
                        // hpai>
                        dialog.sendRequest(ct);
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                    System.out.println("Unexpected exception ");
                }

            }

        }

        // <hpai
        class ByeTask1 extends TimerTask {

            public void run() {
                try {
                    if (dialog != null && dialog.getState() != DialogState.TERMINATED) {
                        Request byeRequest = dialog.createRequest(Request.BYE);
                        if (byeRequest.getHeader(RouteHeader.NAME) != null) {
                            byeRequest.removeHeader(RouteHeader.NAME);
                        }
                        // <hpai
                        String toUser = "LittleGuy";
                        SipURI requestURI1 = addressFactory.createSipURI(toUser,
                                "192.168.100.102:5080");
                        byeRequest.setRequestURI(requestURI1);
                        ClientTransaction ct1 = sipProvider
                                .getNewClientTransaction(byeRequest);
                        System.out.println(">>>>>>> BYE1 Message: " + byeRequest.toString());
                        dialog.sendRequest(ct1);
                        // hpai>
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                    System.out.println("Unexpected exception ");
                }

            }

        }
        // hpai>

        public Shootist(String stackName) {
            this.stackName = stackName;
        }

        public void processRequest(RequestEvent requestReceivedEvent) {
            Request request = requestReceivedEvent.getRequest();
            ServerTransaction serverTransactionId = requestReceivedEvent
                    .getServerTransaction();

			/*System.out.println("\n\nRequest " + request.getMethod()
					+ " received at " + sipStack.getStackName()
					+ " with server transaction id " + serverTransactionId);
			*/

            // We are the UAC so the only request we get is the BYE.
            if (request.getMethod().equals(Request.BYE))
                processBye(request, serverTransactionId);
            else {
                if (!request.getMethod().equals(Request.ACK)) {
                    // not used in basic reinvite
                    if (((CSeqHeader) request.getHeader(CSeqHeader.NAME))
                            .getSeqNumber() == 1
                            && ((ToHeader) request.getHeader(ToHeader.NAME))
                            .getAddress().getURI().toString()
                            .contains("ReInviteSubsNotify")) {
                        try {
                            serverTransactionId.sendResponse(messageFactory
                                    .createResponse(202, request));
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.out.println("Unxepcted exception ");
                        }
                    } else {
                        processInvite(requestReceivedEvent, serverTransactionId);
                    }
                } else {
                    if (request.getMethod().equals(Request.ACK)) {

                    }
                }
            }

        }

        /**
         * Process the invite request.
         */
        public void processInvite(RequestEvent requestEvent,
                                  ServerTransaction serverTransaction) {
            SipProvider sipProvider = (SipProvider) requestEvent.getSource();
            Request request = requestEvent.getRequest();
            if (!((ToHeader) requestEvent.getRequest().getHeader(ToHeader.NAME))
                    .getAddress().getURI().toString().contains("ReInvite")
                    && !((FromHeader) requestEvent.getRequest().getHeader(
                    FromHeader.NAME)).getAddress().getURI().toString()
                    .contains("LittleGuy")) {
                throw new IllegalStateException(
                        "The From and To Headers are reversed !!!!");
            }
            try {
                System.out.println("shootme: got an Invite sending Trying");
                // System.out.println("shootme: " + request);
                Response response = messageFactory.createResponse(
                        Response.RINGING, request);
                ServerTransaction st = requestEvent.getServerTransaction();

                if (st == null) {
                    st = sipProvider.getNewServerTransaction(request);
                }
                dialog = st.getDialog();
                st.sendResponse(response);

                Thread.sleep(1000);

                Response okResponse = messageFactory.createResponse(
                        Response.OK, request);
                Address address = addressFactory.createAddress("Shootme <sip:"
                        + myAddress + ":" + myPort + ">");
                ContactHeader contactHeader = headerFactory
                        .createContactHeader(address);
                response.addHeader(contactHeader);
                okResponse.addHeader(contactHeader);
                // this.inviteTid = st;
                // Defer sending the OK to simulate the phone ringing.
                // Answered in 1 second ( this guy is fast at taking calls)
                // this.inviteRequest = request;

                if (inviteTid.getState() != TransactionState.COMPLETED) {
                    System.out.println("shootme: Dialog state before 200: "
                            + inviteTid.getDialog().getState());
                    st.sendResponse(okResponse);
                    System.out.println("shootme: Dialog state after 200: "
                            + inviteTid.getDialog().getState());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(0);
            }
        }

        public void processBye(Request request,	ServerTransaction serverTransactionId) {
            try {
                System.out.println("Shootist:  got a bye .");
                if (serverTransactionId == null) {
                    System.out.println("Shootist:  null TID.");
                    return;
                }
                Dialog dialog = serverTransactionId.getDialog();
                System.out.println("Dialog State = " + dialog.getState());
                // check route
                if (recordRouteAddr != null) {
                    RouteHeader route = (RouteHeader)request.getHeader(RouteHeader.NAME);
                    //                    assertNotNull(route);
                    //                    assertEquals(recordRouteAddr, route.getAddress().getURI().toString());
                }
                System.out.println("Shootist:  Sending OK.");
                System.out.println("Dialog State = " + dialog.getState());
                Response response = messageFactory.createResponse(200, request);
                serverTransactionId.sendResponse(response);

            } catch (Exception ex) {
                System.out.println("Unexpected exception");

            }
        }

        public void processResponse(ResponseEvent responseReceivedEvent) {
            Response response = (Response) responseReceivedEvent.getResponse();
            ClientTransaction tid = responseReceivedEvent.getClientTransaction();
            CSeqHeader cseq = (CSeqHeader) response.getHeader(CSeqHeader.NAME);

            System.out.println("Shootist: response received -> Status Code = "
                    + response.getStatusCode() + " " + cseq);

            if (tid == null) {

                // RFC3261: MUST respond to every 2xx
                if (ackRequest != null && dialog != null) {
                    System.out.println("re-sending ACK");
                    try {
                        dialog.sendAck(ackRequest);

                    } catch (SipException se) {
                        se.printStackTrace();
                        System.out.println("Unxpected exception ");
                    }
                }
                return;
            }
            System.out.println("Shootist: dialog State is " + tid.getDialog().getState());

            try {
                if (response.getStatusCode() == Response.OK) {
                    if (cseq.getMethod().equals(Request.INVITE)) {
                        System.out.println("Shootist: dialog after 200 OK  " + dialog);
                        System.out.println("Shootist: dialog State after 200 OK  "
                                + dialog.getState());
                        Request ackRequest = dialog.createAck(cseq.getSeqNumber());
                        if (ackRequest.getHeader(RouteHeader.NAME) != null) {
                            ackRequest.removeHeader(RouteHeader.NAME);
                        }
                        System.out.println("Shootist: sending ACK");
                        dialog.sendAck(ackRequest);

                    } else if (cseq.getMethod().equals(Request.CANCEL)) {
                        if (dialog.getState() == DialogState.CONFIRMED) {
                            // oops cancel went in too late. Need to hang up the
                            // dialog.
                            System.out
                                    .println("Shootist: sending BYE -- cancel went in too late !!");
                            Request byeRequest = dialog
                                    .createRequest(Request.BYE);
                            ClientTransaction ct = sipProvider
                                    .getNewClientTransaction(byeRequest);
                            dialog.sendRequest(ct);

                        }

                    } else if (cseq.getMethod().equals(Request.BYE)) {
                        okToByeReceived = true;
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(0);
            }

        }

        public void processTimeout(javax.sip.TimeoutEvent timeoutEvent) {
            System.out.println("Shootist: transaction Timed out");
        }

        public void sendCancel() {
            try {
                System.out.println("Shootist: sending cancel");
                Request cancelRequest = inviteTid.createCancel();
                ClientTransaction cancelTid = sipProvider
                        .getNewClientTransaction(cancelRequest);
                cancelTid.sendRequest();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        public void sendBye() {
            System.out.println("Shootist: sending bye");
            new Timer().schedule(new ByeTask(), 100);
        }

        // <hpai
        public void sendBye1() {
            System.out.println("Shootist: sending bye");
            new Timer().schedule(new ByeTask1(), 100);
        }
        // hpai>

        public void init(String from, String peer) {
            SipFactory sipFactory = null;
            sipStack = null;
            sipFactory = SipFactory.getInstance();
            sipFactory.setPathName("gov.nist");
            Properties properties = new Properties();
            String transport = "udp";
            String peerHostPort = peer;
            properties.setProperty("javax.sip.STACK_NAME", stackName);

            properties.setProperty("gov.nist.javax.sip.DEBUG_LOG",
                    "logs/shootistdebug.txt");
            properties.setProperty("gov.nist.javax.sip.SERVER_LOG",
                    "logs/shootistlog.xml");

            // Drop the client connection after we are done with the
            // transaction.
            properties.setProperty(
                    "gov.nist.javax.sip.CACHE_CLIENT_CONNECTIONS", "false");
            // Set to 0 (or NONE) in your production code for max speed.
            // You need 16 (or TRACE) for logging traces. 32 (or DEBUG) for
            // debug + traces.
            // Your code will limp at 32 but it is best for debugging.
            properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "DEBUG");

            try {
                // Create SipStack object
                sipStack = sipFactory.createSipStack(properties);

            } catch (PeerUnavailableException e) {
                // could not find gov.nist.jain.protocol.ip.sip.SipStackImpl
                // in the classpath
                e.printStackTrace();
                System.err.println(e.getMessage());
                System.exit(0);
            }

            try {
                headerFactory = sipFactory.createHeaderFactory();
                addressFactory = sipFactory.createAddressFactory();
                messageFactory = sipFactory.createMessageFactory();
                udpListeningPoint = sipStack.createListeningPoint(myAddress,
                        myPort, "udp");
                sipProvider = sipStack.createSipProvider(udpListeningPoint);
                Shootist listener = this;
                sipProvider.addSipListener(listener);

                String fromName = from;
                String fromSipAddress = "here.com";
                String fromDisplayName = "The Master Blaster";

                String toSipAddress = "there.com";
                String toUser = "LittleGuy";
                String toDisplayName = "The Little Blister";

                // create From Header
                SipURI fromAddress = addressFactory.createSipURI(fromName,
                        fromSipAddress);

                Address fromNameAddress = addressFactory
                        .createAddress(fromAddress);
                fromNameAddress.setDisplayName(fromDisplayName);
                FromHeader fromHeader = headerFactory.createFromHeader(
                        fromNameAddress, "12345");

                // create To Header
                SipURI toAddress = addressFactory.createSipURI(toUser,
                        toSipAddress);
                Address toNameAddress = addressFactory.createAddress(toAddress);
                toNameAddress.setDisplayName(toDisplayName);
                ToHeader toHeader = headerFactory.createToHeader(toNameAddress,
                        null);

                // create Request URI
                SipURI requestURI = addressFactory.createSipURI(toUser,
                        peerHostPort);

                // Create ViaHeaders
                ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
                String ipAddress = udpListeningPoint.getIPAddress();
                ViaHeader viaHeader = headerFactory.createViaHeader(ipAddress,
                        sipProvider.getListeningPoint(transport).getPort(),
                        transport, null);

                // Add via headers
                viaHeaders.add(viaHeader);

                // Create ContentTypeHeader
                ContentTypeHeader contentTypeHeader = headerFactory
                        .createContentTypeHeader("application", "sdp");

                // Create a new CallId header
                CallIdHeader callIdHeader = sipProvider.getNewCallId();

                // Create a new Cseq header
                CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(1L, Request.INVITE);

                // Create a new MaxForwardsHeader
                MaxForwardsHeader maxForwards = headerFactory
                        .createMaxForwardsHeader(70);

                // Create the request.
                Request request = messageFactory.createRequest(requestURI,
                        Request.INVITE, callIdHeader, cSeqHeader, fromHeader,
                        toHeader, viaHeaders, maxForwards);
                // Create contact headers
                String host = myAddress;

                SipURI contactUrl = addressFactory.createSipURI(fromName, host);
                contactUrl.setPort(udpListeningPoint.getPort());
                contactUrl.setLrParam();

                // Create the contact name address.
                SipURI contactURI = addressFactory.createSipURI(fromName, host);
                contactURI.setPort(sipProvider.getListeningPoint(transport)
                        .getPort());

                Address contactAddress = addressFactory
                        .createAddress(contactURI);

                // Add the contact address.
                contactAddress.setDisplayName(fromName);

                contactHeader = headerFactory
                        .createContactHeader(contactAddress);
                request.addHeader(contactHeader);

                // Add RecordRoute
                if (recordRouteAddr != null) {
                    System.out.println("Shootist: add Record-route: " + recordRouteAddr);
                    Address rrAddr = addressFactory.createAddress(recordRouteAddr);
                    RecordRouteHeader rr = headerFactory.createRecordRouteHeader(rrAddr);
                    request.addHeader(rr);
                }

                String sdpData = "v=0\r\n"
                        + "o=4855 13760799956958020 13760799956958020"
                        + " IN IP4  129.6.55.78\r\n"
                        + "s=mysession session\r\n" + "p=+46 8 52018010\r\n"
                        + "c=IN IP4  129.6.55.78\r\n" + "t=0 0\r\n"
                        + "m=audio 6022 RTP/AVP 0 4 18\r\n"
                        + "a=rtpmap:0 PCMU/8000\r\n"
                        + "a=rtpmap:4 G723/8000\r\n"
                        + "a=rtpmap:18 G729A/8000\r\n" + "a=ptime:20\r\n";
                byte[] contents = sdpData.getBytes();

                request.setContent(contents, contentTypeHeader);

                // Create the client transaction.
                inviteTid = sipProvider.getNewClientTransaction(request);

                // send the request out.
                inviteTid.sendRequest();

                dialog = inviteTid.getDialog();

                if(dialog == null) {
                    System.out.println("Error dialog is null!!!");
                }

            } catch (Exception ex) {
                System.out.println(ex.getMessage());
                ex.printStackTrace();
                System.out.println("Unxpected exception ");
            }
        }

        public void processIOException(IOExceptionEvent exceptionEvent) {
            System.out.println("IOException happened for "
                    + exceptionEvent.getHost() + " port = "
                    + exceptionEvent.getPort());

        }

        public void processTransactionTerminated(
                TransactionTerminatedEvent transactionTerminatedEvent) {
        }

        public void processDialogTerminated(
                DialogTerminatedEvent dialogTerminatedEvent) {
        }

        public void stop() {
            sipStack.stop();
        }

        /**
         * Checks Bye OK successfully received
         */
        public void checkState() {
            if (okToByeReceived) {
                System.out.println("Shootist call terminated successfully");
            } else {
                System.out.println("No response to Bye received");
            }
        }

        public void addRecordRoute(String rr) {
            recordRouteAddr = rr;
        }
    }


    /**
     * SHOTIST1           SHOOTME1         SHOOTME2
     * INVITE ---------------->
     * 	 <---------------- 200 OK
     * ACK ------------------->
     *              stop #1 & create #2
     * BYE ----------------------------------->
     * <--------------------------------- 200 OK
     */
    public void testByeFromShootist() throws Exception {

        BasicConfigurator.configure();
        //Logger.getRootLogger().removeAllAppenders();

        System.out.println("\r\n>>>>>>>>>> Shootist sends BYE <<<<<<<<<<<\r\n");

        String recordRoute = "sip:some.domain.com:5090;lr";
        // create invite initiator
        Shootist shootist = new Shootist("shootist");
        shootist.addRecordRoute(recordRoute);

        // create and start first receiver
        Shootme shootme1 = new Shootme("shootme1", "jain-sip-ha1", 5070, true, ReplicationStrategy.ConfirmedDialog);
        System.out.println(">>>> Start Shootme1");
        Thread.sleep(1000);
        shootme1.init();

        // get dialogs cache created by shootme1
        //        DefaultCacheManager cm = CacheManagerHolder.getManager("META-INF/cache-configuration.xml");
        //        Cache<String, Object> dialogs = cm.getCache("cache.dialogs");
        //        Cache<String, Object> appData = cm.getCache("cache.appdata");


        // start test sending an invite
        System.out.println(">>>> Start Shootist");
        shootist.init("shootist", "192.168.100.111:5070"); // shoot peer1

        Thread.sleep(2000);

        // check first transacion completed
        //        shootme1.checkState();

        // compare dialog metadata with cache metadata
        // <hpai
        //        String dialogId = shootme1.dialog.getDialogId();
        //        System.out.println(">>>> dialogId = " + dialogId);
        // hpai>
        //        Map<String, Object> cachedMetaData = (Map<String, Object>) dialogs
        //                .get(shootme1.dialog.getDialogId());
        //        Object data = appData.get(shootme1.dialog.getDialogId());

        //        assertNotNull(dialogId);
        //        assertNotNull(cachedMetaData);
        //        assertEquals(shootme1.dialog.getLocalTag(),
        //                cachedMetaData.get(AbstractHASipDialog.LOCAL_TAG));
        //        assertEquals(shootme1.dialog.getRemoteTag(),
        //                cachedMetaData.get(AbstractHASipDialog.REMOTE_TAG));
        //        assertEquals(shootme1.dialog.getRemoteTarget().toString(),
        //                cachedMetaData.get(AbstractHASipDialog.REMOTE_TARGET));
        //        String [] routeList = (String[])cachedMetaData.get(AbstractHASipDialog.ROUTE_LIST);
        //        assertNotNull(routeList);
        //        assertEquals(1, routeList.length);
        //        assertEquals("<"+recordRoute+">", routeList[0]);
        //
        //        String txId = (String)shootme1.dialog.getApplicationData();
        //        assertNotNull(txId);
        //        assertNotNull(data);
        //        assertEquals(txId, (String)data);

        // kill shootme
        //        shootme1.stop();
        //        shootme1 = null;

        //        System.out.println(">>>> Kill Shootme1. Dialog cached succesfully.");

        // ---- Recover dialog in new shootme instance ----
        //        Shootme shootme2 = new Shootme("shootme2", "jain-sip-ha1", 5070, true, ReplicationStrategy.ConfirmedDialog);
        //        System.out.println(">>>> Start Shootme2");
        //        Thread.sleep(1000);

        // start shootme2
        //        shootme2.init();

        Thread.sleep(20000);

        // send bye should recover dialog and send 200 ok response
        shootist.sendBye();

        // <hpai
        Thread.sleep(5000);
        shootist.sendBye1();
        // hpai>

        Thread.sleep(100000);

        // <hpai
        Thread.sleep(1000);
        // hpai>

        // check successfull bye
        shootist.checkState();

        System.out.println(">>>> Call recovered succesfully.");

        // clean resources
        shootist.stop();
        //        shootme2.stop();
        //        shootme2 = null;
        shootist = null;
        Thread.sleep(5000);
    }

    public void testByeFromShootme() throws Exception {

        BasicConfigurator.configure();
        //Logger.getRootLogger().removeAllAppenders();

        System.out.println("\r\n>>>>>>>>>> Shootist Waiting BYE <<<<<<<<<<<\r\n");

        // create and start first receiver
        Shootme shootme1 = new Shootme("shootme1", "jain-sip-ha1", 5070, true, ReplicationStrategy.ConfirmedDialog);
        System.out.println(">>>> Start Shootme1");
        Thread.sleep(1000);
        shootme1.init();

        Thread.sleep(200000);

        String dialogId = shootme1.dialog.getDialogId();
        System.out.println(">>>> dialogId = " + dialogId);
    }

    public void testMessageTransaction() throws Exception {

        BasicConfigurator.configure();
        //Logger.getRootLogger().removeAllAppenders();

        System.out.println("\r\n>>>>>>>>>> Shootist Waiting BYE <<<<<<<<<<<\r\n");

        // create and start first receiver
        Shootme shootme1 = new Shootme("shootme1", "jain-sip-ha1", 5070, true, ReplicationStrategy.ConfirmedDialog);
        System.out.println(">>>> Start Shootme message");
        Thread.sleep(1000);
        shootme1.init();

        Thread.sleep(5000);


        SipClient sipClient;
        try {
            sipClient = new SipClient();
            System.out.println("SipClient Started.");
            sipClient.sendSubscribe("192.168.100.111", 5070);
        } catch (Exception e) {
            System.out.println("Start SipClient fail!!!" + e.toString());
        }

        Thread.sleep(3600000);

        String dialogId = shootme1.dialog.getDialogId();
        System.out.println(">>>> dialogId = " + dialogId);
    }

    public void testRecoverSubscribeDialog(String dialogId) throws InterruptedException {
        BasicConfigurator.configure();
        //Logger.getRootLogger().removeAllAppenders();

        System.out.println("\r\n>>>>>>>>>> Shootist Waiting BYE <<<<<<<<<<<\r\n");

        // create and start first receiver
        Shootme shootme1 = new Shootme("shootme1", "jain-sip-ha1", 5070, true, ReplicationStrategy.ConfirmedDialog);
        System.out.println(">>>> Start Shootme message");
        Thread.sleep(1000);
        shootme1.init();

        Thread.sleep(5000);

        System.out.println("time 15s");
        Dialog tmpDialog1 = ((SipStackImpl)shootme1.getSipStack()).getDialog(dialogId);
        SessionCache sessionCache1 =(SessionCache) tmpDialog1.getApplicationData();
        System.out.println("SessionCache.getSessionId: " + sessionCache1.getSessionId());
        System.out.println("SessionCache.getTransactionId: " + sessionCache1.getTransactionId());
        System.out.println("SessionCache.getCurrentState: " + sessionCache1.getCurrentState());
        System.out.println("SessionCache.getContents: " + sessionCache1.getContents());


        Thread.sleep(200000);

        System.out.println("time 215s");
        Dialog tmpDialog2 = ((SipStackImpl)shootme1.getSipStack()).getDialog(dialogId);
        SessionCache sessionCache2 =(SessionCache) tmpDialog2.getApplicationData();
        System.out.println("SessionCache.getSessionId: " + sessionCache2.getSessionId());
        System.out.println("SessionCache.getTransactionId: " + sessionCache2.getTransactionId());
        System.out.println("SessionCache.getCurrentState: " + sessionCache2.getCurrentState());
        System.out.println("SessionCache.getContents: " + sessionCache2.getContents());

        Thread.sleep(2000000);
        System.out.println("time 2215s");
        Dialog tmpDialog3 = ((SipStackImpl)shootme1.getSipStack()).getDialog(dialogId);
        SessionCache sessionCache4 =(SessionCache) tmpDialog3.getApplicationData();
        System.out.println("SessionCache.getSessionId: " + sessionCache4.getSessionId());
        System.out.println("SessionCache.getTransactionId: " + sessionCache4.getTransactionId());
        System.out.println("SessionCache.getCurrentState: " + sessionCache4.getCurrentState());
        System.out.println("SessionCache.getContents: " + sessionCache4.getContents());
    }

    public static void main(String[] args) {

        JainSipHaDemo testCase = new JainSipHaDemo();
        try {
            testCase.testMessageTransaction();
        } catch (Exception e) {
            System.out.println("Start testMessageTransaction fail!!!" + e.toString());
        }


    }

}
/**
 * Revision history
 * -------------------------------------------------------------------------
 *
 * Date Author Note
 * -------------------------------------------------------------------------
 * 2018年03月08日 09:22 hpai create
 */
