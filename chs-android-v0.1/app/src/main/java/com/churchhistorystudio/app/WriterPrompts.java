package com.churchhistorystudio.app;

final class WriterPrompts {
  static String system() {
    return WriterPromptsV044.system();
  }
  static String user(String payloadJson) throws Exception {
    return WriterPromptsV044.user(payloadJson);
  }
}
