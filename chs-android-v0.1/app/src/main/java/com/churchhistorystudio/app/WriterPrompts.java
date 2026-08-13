package com.churchhistorystudio.app;

final class WriterPrompts {
  static String system() { return WriterPromptsV041.system(); }
  static String user(String payloadJson) throws Exception { return WriterPromptsV041.user(payloadJson); }
}
