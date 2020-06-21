package app.web.pavelk.cloud1.common.call;

public interface ClientMainContentsCallback {
    void autOkClient();
    void autNotOkClient();
    void updateContentsCallBackClient(String name, long len);
    void clearContentsCallBackClient();
    void clientUpdateContents();
}


