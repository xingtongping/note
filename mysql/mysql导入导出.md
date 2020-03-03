### mysql导入导出

导出例子

/home/workspace/mysql/bin/mysqldump -h 127.0.0.1 -uroot -proot --default-character-set=utf8 xxl_job --ignore-table=xxl_job.xxl_job_log > /home/workspace/mysql/backdir/xxl_job.sql

导入例子

/home/workspace/mysql/bin/mysql -uroot -proot xxl_job <  /home/workspace/mysql/backdir/xxl_job.sql