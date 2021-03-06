package org.recap.ils;

import com.ceridwen.circulation.SIP.exceptions.*;
import com.ceridwen.circulation.SIP.messages.ACSStatus;
import com.ceridwen.circulation.SIP.messages.ItemInformation;
import com.ceridwen.circulation.SIP.messages.Message;
import com.ceridwen.circulation.SIP.messages.SCStatus;
import com.ceridwen.circulation.SIP.transport.SocketConnection;
import com.ceridwen.circulation.SIP.types.enumerations.ProtocolVersion;
import com.ceridwen.circulation.SIP.types.flagfields.SupportedMessages;

/**
 * Created by saravanakumarp on 22/9/16.
 */
public abstract class ESIPConnector {

    public Message checkOut(String institionId, String itemIdentifier, java.util.Date transactionDate) {
        Message request, response;
        SocketConnection connection = getSocketConnection();
        if (connection == null) return null;

        /**
         * It is necessary to send a SC Status with protocol version 2.0 else
         * server will assume 1.0)
         */
        request = new SCStatus();
        ((SCStatus) request).setProtocolVersion(ProtocolVersion.VERSION_2_00);

        response = getResponse(request, connection);
        if (!(response instanceof ACSStatus)) {
            System.err.println("Error - Status Request did not return valid response from server.");
            return null;
        }

        /**
         * For debugging XML handling code (but could be useful in Cocoon)
         */
        //response.xmlEncode(System.out);

        /**
         * Check if the server can support checkout
         */
        if (!((ACSStatus) response).getSupportedMessages().isSet(SupportedMessages.CHECK_OUT)) {
            System.out.println("Check out not supported");
            return null;
        }

        ItemInformation itemInformation = new ItemInformation();

        /**
         * The code below would be the normal way of creating the request
         */
        itemInformation.setInstitutionId(institionId);
        itemInformation.setItemIdentifier(itemIdentifier);
        itemInformation.setTransactionDate(transactionDate);

        response = getResponse(itemInformation, connection);
        response.xmlEncode(System.out);
        connection.disconnect();
        return response;
    }

    private Message getResponse(Message request, SocketConnection connection) {
        Message response;
        try {
            response = connection.send(request);
        } catch (RetriesExceeded e) {
            e.printStackTrace();
            return null;
        } catch (ConnectionFailure e) {
            e.printStackTrace();
            return null;
        } catch (MessageNotUnderstood e) {
            e.printStackTrace();
            return null;
        } catch (ChecksumError e) {
            e.printStackTrace();
            return null;
        } catch (SequenceError e) {
            e.printStackTrace();
            return null;
        } catch (MandatoryFieldOmitted e) {
            e.printStackTrace();
            return null;
        } catch (InvalidFieldLength e) {
            e.printStackTrace();
            return null;
        }
        return response;
    }

    private SocketConnection getSocketConnection() {
        SocketConnection connection =new SocketConnection();

        connection.setHost(getHost());
        connection.setPort(7031);
        connection.setConnectionTimeout(30000);
        connection.setIdleTimeout(30000);
        connection.setRetryAttempts(2);
        connection.setRetryWait(500);

        try {
            connection.connect();
        } catch (Exception e1) {
            e1.printStackTrace();
            return null;
        }
        return connection;
    }

    public abstract String getHost();

}

