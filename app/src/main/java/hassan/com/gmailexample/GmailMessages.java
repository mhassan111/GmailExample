package hassan.com.gmailexample;

import android.util.Log;

import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

/**
 * Created by ut on 8/7/2017.
 */

public class GmailMessages {
    public static void getMessages(Gmail mService, String user, List<String> labels) {
        ListMessagesResponse response = null;
        try {
            response = mService.users().messages().list(user).setLabelIds(labels).setMaxResults(Long.parseLong("10")).execute();
            List<Message> messages = new ArrayList<>();
            messages.addAll(response.getMessages());
            for (Message message : messages) {
                Message message1 = mService.users().messages().get(user, message.getId()).setFormat("raw").execute();
                Log.d("message = ", message1.toPrettyString());
                Map<String, Object> messageDetails = new HashMap<String, Object>();
                byte[] emailBytes = Base64.decodeBase64(message1.getRaw());
                Properties props = new Properties();
                Session session = Session.getDefaultInstance(props, null);
                try {
                    MimeMessage email = new MimeMessage(session, new ByteArrayInputStream(emailBytes));
                    messageDetails.put("subject", email.getSubject());
                    messageDetails.put("from", email.getSender() != null ? email.getSender().toString() : "None");
                    messageDetails.put("time", email.getSentDate() != null ? email.getSentDate().toString() : "None");
                    messageDetails.put("snippet", message1.getSnippet());
                    messageDetails.put("threadId", message1.getThreadId());
                    messageDetails.put("id", message1.getId());
                    messageDetails.put("body", getText(email));
                } catch (MessagingException e) {
                    e.printStackTrace();
                }
                Log.d("message details = ", messageDetails.toString());
            }
        } catch (UserRecoverableAuthIOException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.getMessage();
        }
    }

    private static boolean textIsHtml = false;

    /**
     * Return the primary text content of the message.
     */
    private static String getText(Part p) throws
            MessagingException, IOException {
        if (p.isMimeType("text/*")) {
            String s = (String) p.getContent();
            textIsHtml = p.isMimeType("text/html");
            return s;
        }

        if (p.isMimeType("multipart/alternative")) {
            // prefer html text over plain text
            Multipart mp = (Multipart) p.getContent();
            String text = null;
            for (int i = 0; i < mp.getCount(); i++) {
                Part bp = mp.getBodyPart(i);
                if (bp.isMimeType("text/plain")) {
                    if (text == null) {
                        text = getText(bp);
                    }
                    continue;
                } else if (bp.isMimeType("text/html")) {
                    String s = getText(bp);
                    if (s != null) {
                        return s;
                    }
                } else {
                    return getText(bp);
                }
            }
            return text;
        } else if (p.isMimeType("multipart/*")) {
            Multipart mp = (Multipart) p.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                String s = getText(mp.getBodyPart(i));
                if (s != null) {
                    return s;
                }
            }
        }
        return null;
    }

}
