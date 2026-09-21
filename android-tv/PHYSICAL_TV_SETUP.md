# Physical Android TV setup

1. On the TV: Settings > About, press Build/Android TV OS build repeatedly until Developer Options are enabled.
2. Enable **Wireless debugging** (or Network/ADB debugging on older TVs).
3. On Android 11+ TV, choose **Pair device with pairing code**. It shows `IP:PAIRING_PORT` and a six-digit code.
4. Windows PowerShell:
   `cd android-tv\scripts`
   `.\connect-tv.ps1 192.168.1.50:37123 -PairCode 123456`
5. The TV's Wireless debugging screen also shows an `IP:ADB_PORT`. Connect:
   `.\connect-tv.ps1 192.168.1.50:42177`
6. From then on, update + launch RedPlay with:
   `.\install-tv.ps1`

On older TVs that expose classic TCP ADB on port 5555, use `.\connect-tv.ps1 192.168.1.50:5555`.
