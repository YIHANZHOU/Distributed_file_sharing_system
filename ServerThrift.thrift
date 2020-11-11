struct Address {
  1: string ip,
  2: i32 port,
}
service ServerThrift {
  bool join(1:string ip, 2:i32 port),
  string ReadfromClient(1:string fileName),
  i32 VersionNumber(1:string fileName),
  string ReadFile(1:string fileName),
  string WriteFile(1:string fileName,2:string contents,3:i32 VersionNumber),
  string ForwardRead(1:string fileName)
}
