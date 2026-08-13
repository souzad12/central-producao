package com.churchhistorystudio.app;

final class WriterPrompts {
  static String system() { return WriterPromptsV045.system(); }
  static String user(String payloadJson) throws Exception { return WriterPromptsV045.user(payloadJson); }
}
