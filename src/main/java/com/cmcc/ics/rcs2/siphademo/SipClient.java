package com.cmcc.ics.rcs2.siphademo;

import java.util.ArrayList;
import java.util.Properties;

import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.ListeningPoint;
import javax.sip.PeerUnavailableException;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.SipFactory;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.address.TelURL;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.ExpiresHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.Header;
import javax.sip.header.HeaderFactory;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;

import gov.nist.javax.sip.stack.SIPClientTransaction;

/**
 * @Type SipClient
 * @Desc SipClient
 * @author hpai
 * @date 2018年03月20日 09:23
 * @version
 */
public class SipClient implements SipListener {

    private SipProvider sipProvider;

    private static AddressFactory addressFactory;

    private static MessageFactory messageFactory;

    private static HeaderFactory headerFactory;

    private static SipStack sipStack;

    private ContactHeader contactHeader;

    private static String transport;

    private ClientTransaction transaction;

    private ListeningPoint listeningPoint;

    private static void init() throws Exception {
        System.out.println("start SipClientTest init");
        transport = "udp";

        SipFactory sipFactory = SipFactory.getInstance();
        sipFactory.setPathName("gov.nist");
        Properties properties = new Properties();

        properties.setProperty("javax.sip.USE_ROUTER_FOR_ALL_URIS", "false");
        properties.setProperty("javax.sip.STACK_NAME", "RegisterClient");
        properties.setProperty("gov.nist.javax.sip.DEBUG_LOG", "RegisterClient.txt");
        properties.setProperty("gov.nist.javax.sip.SERVER_LOG", "RegisterClientLog.txt");
        properties.setProperty("javax.sip.FORKABLE_EVENTS", "foo");
        properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "32");

        try {
            sipStack = sipFactory.createSipStack(properties);
        } catch (PeerUnavailableException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println("createSipStack " + sipStack);
        headerFactory = sipFactory.createHeaderFactory();
        addressFactory = sipFactory.createAddressFactory();
        messageFactory = sipFactory.createMessageFactory();
    }

    public SipClient() throws Exception {
        init();
        createProvider();
        sipProvider.addSipListener(this);
    }

    public void createProvider() throws Exception {

        this.listeningPoint = sipStack.createListeningPoint("192.168.100.111", 6070, "udp");
        sipProvider = sipStack.createSipProvider(listeningPoint);

    }

    @Override
    public void processRequest(RequestEvent requestEvent) {

    }

    @Override
    public void processResponse(ResponseEvent responseEvent) {
        System.out.println("Receive SIP Response: " + responseEvent.getResponse().toString());
        Dialog dialog = responseEvent.getDialog();
        if(dialog != null) {
            System.out.println("##--dialog id: " + dialog.getDialogId());
        }
    }

    @Override
    public void processTimeout(TimeoutEvent timeoutEvent) {

    }

    @Override
    public void processIOException(IOExceptionEvent ioExceptionEvent) {

    }

    @Override
    public void processTransactionTerminated(TransactionTerminatedEvent transactionTerminatedEvent) {

    }

    @Override
    public void processDialogTerminated(DialogTerminatedEvent dialogTerminatedEvent) {

    }

    String messageCpimContent = "From: <sip:+8614715008380@gd.ims.mnc000.mcc460.3gppnetwork.org>" + SipConstant.CRLF
            + "To: <tel:+8614715008383>" + SipConstant.CRLF
            + "NS: imdn<urn:ietf:params:imdn>" + SipConstant.CRLF
            + "imdn.Message-ID: XXdcbQV7670XKNzx" + SipConstant.CRLF
            + "DateTime: 2015-11-16T14:02:14.899+08:00" + SipConstant.CRLF
            + "imdn.Disposition-Notification: positive-delivery, negative-delivery, display" + SipConstant.CRLF
            + SipConstant.CRLF
            + "Content-type: text/plain;charset=UTF-8" + SipConstant.CRLF
            + "Content-Transfer-Encoding: base64" + SipConstant.CRLF
            + "Content-length: 16" + SipConstant.CRLF
            + SipConstant.CRLF
            + "57qi57qi54Gr54Gr" + SipConstant.CRLF;

    String inviteCpimContent = "From: <sip:+8614715008380@gd.ims.mnc000.mcc460.3gppnetwork.org>" + SipConstant.CRLF
            + "To: <tel:+8614715008383>" + SipConstant.CRLF
            + "NS: imdn<urn:ietf:params:imdn>" + SipConstant.CRLF
            + "imdn.Message-ID: W0dcbQV7670XKNzx" + SipConstant.CRLF
            + "DateTime: 2015-11-16T14:06:07.550+08:00" + SipConstant.CRLF
            + "imdn.Disposition-Notification: positive-delivery, negative-delivery" + SipConstant.CRLF
            + SipConstant.CRLF
            + "Content-Transfer-Encoding: base64" + SipConstant.CRLF
            + "Content-type: text/plain" + SipConstant.CRLF
            + "Content-length: 0" + SipConstant.CRLF
            + SipConstant.CRLF;

    String inviteSdpContent = "v=0\r\n"
            + "o=- 1562475580 0 IN IP4 10.185.83.129\r\n"
            + "c=IN IP4 10.185.83.129\r\n"
            + "t=0 0\r\n"
            + "m=message 23922 TCP/MSRP *\r\n"
            + "a=setup:actpass\r\n"
            + "a=sendonly\r\n"
            + "a=accept-types:application/octet-stream\r\n"
            + "a=path:msrp://0.0.0.0:9/X1dcbQV767_XKNzx;tcp\r\n"
            + "a=file-selector:name:\"+8617820500132(46)(36).jpg\" type:application/octet-stream size:3623\r\n"
            + "a=file-transfer-id:Y2dcbQV767-XKNzx\r\n";

    String inviteMixedContent = "--SIP_BOUNDARY" + SipConstant.CRLF
            + "Content-Type: message/cpim" + SipConstant.CRLF
            + "Content-Length: 352" + SipConstant.CRLF
            + SipConstant.CRLF
            + "From: <sip:+8614715008380@gd.ims.mnc000.mcc460.3gppnetwork.org>" + SipConstant.CRLF
            + "To: <tel:+8614715008383>" + SipConstant.CRLF
            + "NS: imdn<urn:ietf:params:imdn>" + SipConstant.CRLF
            + "imdn.Message-ID: W0dcbQV7670XKNzx" + SipConstant.CRLF
            + "DateTime: 2015-11-16T14:06:07.550+08:00" + SipConstant.CRLF
            + "imdn.Disposition-Notification: positive-delivery, negative-delivery" + SipConstant.CRLF
            + SipConstant.CRLF
            + "Content-Transfer-Encoding: base64" + SipConstant.CRLF
            + "Content-Type: text/plain" + SipConstant.CRLF
            + "Content-Length: 0" + SipConstant.CRLF
            + SipConstant.CRLF
            + "--SIP_BOUNDARY" + SipConstant.CRLF
            + "Content-Type: application/sdp" + SipConstant.CRLF
            + SipConstant.CRLF
            + "v=0" + SipConstant.CRLF
            + "o=- 1562475580 0 IN IP4 10.185.83.129" + SipConstant.CRLF
            + "c=IN IP4 10.185.83.129" + SipConstant.CRLF
            + "t=0 0" + SipConstant.CRLF
            + "m=message 23922 TCP/MSRP *" + SipConstant.CRLF
            + "a=setup:actpass" + SipConstant.CRLF
            + "a=sendonly" + SipConstant.CRLF
            + "a=accept-types:application/octet-stream" + SipConstant.CRLF
            + "a=path:msrp://0.0.0.0:9/X1dcbQV767_XKNzx;tcp" + SipConstant.CRLF
            + "a=file-selector:name:\"+8617820500132(46)(36).jpg\" type:application/octet-stream size:3623" + SipConstant.CRLF
            + "a=file-transfer-id:Y2dcbQV767-XKNzx" + SipConstant.CRLF
            + SipConstant.CRLF
            + "--SIP_BOUNDARY--" + SipConstant.CRLF;


    public void sendMoMessage(String toSipAddress, int toPoint) {
        try {
            // Destination
            //String toSipAddress = "192.168.100.111"; // "192.168.100.100"; //
            //int toPoint = 5070;

            // Request-Line
            String RequestLine = "MESSAGE tel:+8614715008383 SIP/2.0";
            //String RequestURI = "+8614715008380@ims.mnc008.mcc460.3gppnetwork.org";
            SipURI requestURI = addressFactory.createSipURI(null, RequestLine);
            requestURI.setHost(toSipAddress);
            requestURI.setPort(toPoint);

            // Message Header
            // From Header, tag == null
            String fromSipAddress = "+8613400030074";
            TelURL from = addressFactory.createTelURL(fromSipAddress);
            Address fromNameAddress = addressFactory.createAddress(from);
            System.out.println("fromNameAddress is " + fromNameAddress.toString());
            FromHeader fromHeader = headerFactory.createFromHeader(fromNameAddress, "123456");

            // To Header (Register ToHeader is different)
            String toTel = "<sip:+8613904080@ims.mnc008.mcc460.3gppnetwork.org>";
            ToHeader toHeader = (ToHeader) headerFactory.createHeader("To", toTel);
            //SipURI toAddress = addressFactory.createSipURI(null, toTel);
            //Address toNameAddress = addressFactory.createAddress(toAddress);
            //ToHeader toHeader = headerFactory.createToHeader(toNameAddress, toTel);

            // Call-ID Header
            //CallIdHeader callIdHeader = sipProvider.getNewCallId();
            CallIdHeader callIdHeader = headerFactory.createCallIdHeader("1234567890112233");
            System.out.println("Call-ID header: " + callIdHeader.toString());

            // CSeq Header
            CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(1L, Request.MESSAGE);

            // Max_Forwards Header
            MaxForwardsHeader maxForwardsHeader = headerFactory.createMaxForwardsHeader(68);

            //P-Access-Network_Info
            //PAccessNetworkInfoHeader pAccessNetworkInfoHeader =

            // Content-Type Header
            String contentType = "message";
            String subType = "cpim";
            ContentTypeHeader contentTypeHeader = headerFactory.createContentTypeHeader(contentType,
                    subType);

            // Via Header
            ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
            String host = sipProvider.getListeningPoint(transport).getIPAddress();
            int port = sipProvider.getListeningPoint(transport).getPort();
            ViaHeader viaHeader = headerFactory.createViaHeader(host, port, transport, null);
            viaHeader.setBranch("z9hG4bK*1-1-16648-863-15-235*B_2NK8Y91cdjffef.1");
            viaHeaders.add(viaHeader);

            // Create the request
            Request request = messageFactory.createRequest(requestURI, Request.MESSAGE,
                    callIdHeader, cSeqHeader, fromHeader, toHeader, viaHeaders, maxForwardsHeader);

            // Create contact headers
            SipURI contactUrl = addressFactory.createSipURI(null, host);
            contactUrl.setPort(port);
            Address contactAddress = addressFactory.createAddress(contactUrl);
            contactHeader = headerFactory.createContactHeader(contactAddress);
            request.addHeader(contactHeader);

            // Add Content with contentType
            request.addHeader(contentTypeHeader);
            byte[] moMessageContent = messageCpimContent.getBytes();
            request.setContent(moMessageContent, contentTypeHeader);

            // Contribution-ID Header
            Header contributionId = headerFactory.createHeader(SipConstant.CCSIP_NAME_CONTRIBUTION_ID,
                    "WWdcb4-Brjos7Tzx");
            request.addHeader(contributionId);

            // Conversation-ID Header
            Header conversationId = headerFactory.createHeader(SipConstant.CCSIP_NAME_CONVERSATION_ID,
                    "JVdcb4-Brjos7Tzx");
            request.addHeader(conversationId);

            // Expires Header
            ExpiresHeader expiresHeader = headerFactory.createExpiresHeader(120);
            request.addHeader(expiresHeader);

            // Create the client transaction.
            transaction = sipProvider.getNewClientTransaction(request);

            // send the register request out
            transaction.sendRequest();

            System.out.println("SipClient Dialog = " + transaction.getDialog());
            System.out.println("SipClient Transaction id: " + ((SIPClientTransaction)transaction).getTransactionId());
            System.out.println("SipClient branch: " + transaction.getBranchId());
            System.out.println("SIP request MESSAGE: " + request.toString());

        } catch (Throwable ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();

        }
    }

    public void sendSubscribe(String toSipAddress, int toPoint) {
        try {
            // Destination
            //String toSipAddress = "192.168.100.111"; // "192.168.100.100"; //
            //int toPoint = 5070;

            // Request-Line
            String RequestLine = "SUBSCRIBE tel:+8614715008383 SIP/2.0";
            //String RequestURI = "+8614715008380@ims.mnc008.mcc460.3gppnetwork.org";
            SipURI requestURI = addressFactory.createSipURI(null, RequestLine);
            requestURI.setHost(toSipAddress);
            requestURI.setPort(toPoint);

            // Message Header
            // From Header, tag == null
            String fromSipAddress = "+8613400030074";
            TelURL from = addressFactory.createTelURL(fromSipAddress);
            Address fromNameAddress = addressFactory.createAddress(from);
            System.out.println("fromNameAddress is " + fromNameAddress.toString());
            FromHeader fromHeader = headerFactory.createFromHeader(fromNameAddress, "123456");

            // To Header (Register ToHeader is different)
            String toTel = "<sip:+8613904080@ims.mnc008.mcc460.3gppnetwork.org>";
            ToHeader toHeader = (ToHeader) headerFactory.createHeader("To", toTel);
            //SipURI toAddress = addressFactory.createSipURI(null, toTel);
            //Address toNameAddress = addressFactory.createAddress(toAddress);
            //ToHeader toHeader = headerFactory.createToHeader(toNameAddress, toTel);

            // Call-ID Header
            //CallIdHeader callIdHeader = sipProvider.getNewCallId();
            CallIdHeader callIdHeader = headerFactory.createCallIdHeader("1234567890112233");
            System.out.println("Call-ID header: " + callIdHeader.toString());

            // CSeq Header
            CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(1L, Request.SUBSCRIBE);

            // Max_Forwards Header
            MaxForwardsHeader maxForwardsHeader = headerFactory.createMaxForwardsHeader(68);

            //P-Access-Network_Info
            //PAccessNetworkInfoHeader pAccessNetworkInfoHeader =

            // Content-Type Header
            String contentType = "message";
            String subType = "cpim";
            ContentTypeHeader contentTypeHeader = headerFactory.createContentTypeHeader(contentType,
                    subType);

            // Via Header
            ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
            String host = sipProvider.getListeningPoint(transport).getIPAddress();
            int port = sipProvider.getListeningPoint(transport).getPort();
            ViaHeader viaHeader = headerFactory.createViaHeader(host, port, transport, null);
            viaHeader.setBranch("z9hG4bK*1-1-16648-863-15-235*B_2NK8Y91cdjffef.1");
            viaHeaders.add(viaHeader);

            // Create the request
            Request request = messageFactory.createRequest(requestURI, Request.SUBSCRIBE,
                    callIdHeader, cSeqHeader, fromHeader, toHeader, viaHeaders, maxForwardsHeader);

            // Create contact headers
            SipURI contactUrl = addressFactory.createSipURI(null, host);
            contactUrl.setPort(port);
            Address contactAddress = addressFactory.createAddress(contactUrl);
            contactHeader = headerFactory.createContactHeader(contactAddress);
            request.addHeader(contactHeader);

            // Add Content with contentType
            request.addHeader(contentTypeHeader);
            byte[] moMessageContent = messageCpimContent.getBytes();
            request.setContent(moMessageContent, contentTypeHeader);

            // Contribution-ID Header
            Header contributionId = headerFactory.createHeader(SipConstant.CCSIP_NAME_CONTRIBUTION_ID,
                    "WWdcb4-Brjos7Tzx");
            request.addHeader(contributionId);

            // Conversation-ID Header
            Header conversationId = headerFactory.createHeader(SipConstant.CCSIP_NAME_CONVERSATION_ID,
                    "JVdcb4-Brjos7Tzx");
            request.addHeader(conversationId);

            // Expires Header
            ExpiresHeader expiresHeader = headerFactory.createExpiresHeader(3600);
            request.addHeader(expiresHeader);

            // Create the client transaction.
            if (transaction == null) {
                transaction = sipProvider.getNewClientTransaction(request);
            }

            // send the register request out
            transaction.sendRequest();

            System.out.println("SipClient Dialog = " + transaction.getDialog());
            System.out.println("SipClient Transaction id: " + ((SIPClientTransaction)transaction).getTransactionId());
            System.out.println("SipClient branch: " + transaction.getBranchId());
            System.out.println("SIP request MESSAGE: " + request.toString());

        } catch (Throwable ex) {
            System.out.println(ex.getMessage());
            ex.printStackTrace();

        }
    }
}
/**
 * Revision history
 * -------------------------------------------------------------------------
 *
 * Date Author Note
 * -------------------------------------------------------------------------
 * 2018年03月20日 09:23 hpai create
 */
