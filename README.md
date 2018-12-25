# Alfresco transaction splitter

## DESCRIPTION

## USAGE

1. Stop alfresco

  ```bash
  sudo service alfresco stop
  ```

2. Install the .amp in one of your alfresco installation

  ```bash
  alf_root='/opt/alfresco-content-services'
  sudo ${alf_root}/java/bin/java -jar ${alf_root}/bin/alfresco-mmt.jar install ${alf_root}/amps/transactionSplitter-4.5.amp ${alf_root}/tomcat/webapps/alfresco.war -force
  ```

3. Validate if the the amp was installed

  ```bash
  ${alf_root}/java/bin/java -jar ${alf_root}/bin/alfresco-mmt.jar list ${alf_root}/tomcat/webapps/alfresco.war | head
```

4. Remove alfresco folder

- If you not confortable removing it just move to another place. But you need to move the role folder.

  ```bash
  sudo rm -rf ${alf_root}/tomcat/webapps/alfresco
  sudo rm -rf ${alf_root}/tomcat/temp/*
  ```

It should also be safe to delete other temp folder like.

```bash
  sudo rm -rf ${alf_root}/tomcat/work/*
  sudo rm -rf ${alf_root}/tomcat/webapps/share
  sudo rm -rf ${alf_root}/tomcat/webapps/temp
```

5. Start alfresco

  ```bash
  sudo service alfresco start
  ```

You can tail the logs to check whether it's started properly.

  ```bash
  tailf ${alf_root}/tomcat/logs/catalina.out
  ```

6. Log in in alfresco.
7. Create a folder anywhere with ( i.e. document repository/splitter)
  TODO: add image here
8. Create a folder rule
  TODO: add image here
9.  Run 'run-query.sh'
  Don't forget to adjust the variables in both:
    - run-query.sh
    - get-AlfNodeRef.sql
    If you don't know the transaction_id, run 'get-BigTransactions.sql' it will return the biggest transactions in your database.

    ```bash
    mysql -u  ${db_user} -h ${db_host} ${db_schema} --ssl-ca ${db_ssl_ca} -p < get-BigTransactions.sql
    # or
    mysql -u alfresco@mydatabase -p -h mydatabase.mysql.database.azure.com alfresco --ssl-ca "./azmysql.crt" -e "select distinct
    alft.id as id, alft.commit_time_ms as epoch, (select distinct count(*) from alf_node where transaction_id=alft.id ) as count
    from alf_transaction as alft having count > 500000 order by count desc limit 10;"
    ```

  Test with lower number first.
10. The first splitter document (it will end with aa) you need to delete the first line.

11. Add files into the alfresco folder with the rule enable
  Add files carfully to not overload alfresco by running out of memory.
  
  output error:

  ```output
  There is insufficient memory for the Java Runtime Environment to continue.
  Native memory allocation (mmap) failed to map 2225999872 bytes for committing reserved memory.
  An error report file with more information is saved as:
  /opt/alfresco-content-services/tomcat/logs/hs_err_pid1588.log
  ```

  TODO: Add image.

## Credits
Solution to big transactions was found in
https://community.alfresco.com/thread/178867-relationship-between-alftransaction-and-index-recovery

And the amp file was created based on the project transactionsplitter from Xenit here's the link
https://bitbucket.org/xenit/transactionsplitter

## FAQ

- I uploaded a bunch of files and my system crashed, now I can't upload it again. how to fix it ?
 1. Delete the files that where uploaded;
 2. Stop Alfresco;
 3. Clear temp files (see step usage step 4);
 4. Start Alfresco;
 5. Try again.
