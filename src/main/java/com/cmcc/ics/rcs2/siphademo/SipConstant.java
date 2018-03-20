package com.cmcc.ics.rcs2.siphademo;

/**
 * @Type SipConstant
 * @Desc SipConstant
 * @author hpai
 * @date 2018年03月20日 09:28
 * @version
 */
public interface SipConstant {
    String CCSIP_CONTENT_TYPE_APPLICATION = "application";

    String CCSIP_CONTENT_TYPE_MESSAGE = "message";

    String CCSIP_CONTENT_SUB_TYPE_SDP = "adp";

    String CCSIP_CONTENT_SUB_TYPE_CPIM = "CPIM";

    String CRLF = "\r\n";

    String CCSIP_NAME_CONTRIBUTION_ID = "Contribution-ID";
    String CCSIP_NAME_CONVERSATION_ID = "Conversation-ID";
    String CCSIP_NAME_ACCESS_NETWORK = "P-Access-Network-Info";
    String CCSIP_NAME_SESSION_EXPIRES = "Session-Expires";
    String CCSIP_NAME_MIN_SE = "Min-SE";
    String CCSIP_NAME_PAI = "P-Asserted-Identity";
    String CCSIP_NAME_ACCEPT_CONTACT = "Accept-Contact";


    String CCSIP_ORIGINAL_INSTANCE = "+sip.instance";


    String CCSIP_PARAM_ICSI_REF = "+g.3gpp.icsi-ref";

    String SINGLE_CHAT_FILE_FEATURE_TAG = "*;+g.3gpp.icsi-ref=\"urn%3Aurn-7%3A3gpp-service.ims.icsi.oma.cpm.filetransfer\"";
    String SINGLE_CHAT_FILE_CONTACT_PARAM = "\"urn%3Aurn-7%3A3gpp-service.ims.icsi.oma.cpm.filetransfer\"";
}
/**
 * Revision history
 * -------------------------------------------------------------------------
 *
 * Date Author Note
 * -------------------------------------------------------------------------
 * 2018年03月20日 09:28 hpai create
 */
