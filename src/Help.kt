fun printHelp() {
    println("""
Usage: java -jar app.jar --login LOGIN --password PASSWORD --action ACTION --resource PATH --volume VOLUME

Options:
  --login LOGIN        User login
  --password PASSWORD  User password
  --action ACTION      read, write or execute
  --resource PATH      Resource path, for example A.A8B.C.f_d
  --volume VOLUME      Requested non-negative integer volume
  -h, --help           Show this help
""".trimIndent())
}
