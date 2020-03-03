#### linux时间同步



[root@cdh01 :~ ]# yum install ntp
[root@cdh01 :~ ]# vim /etc/ntp.conf
driftfile /var/lib/ntp/drift
restrict default kod nomodify notrap nopeer noquery
restrict -6 default kod nomodify notrap nopeer noquery
restrict 127.0.0.1
restrict -6 ::1
server localhost1	###ntp时间同步地址
server localhost2	###ntp时间同步地址
includefile /etc/ntp/crypto/pw
keys /etc/ntp/keys
[root@cdh01 :~ ]# systemctl start ntpd
[root@cdh01 :~ ]# systemctl status ntpd
[root@cdh01 :~ ]# systemctl enable ntpd