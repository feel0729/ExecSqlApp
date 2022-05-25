# ExecSqlApp
execute sql script file to multiple environments.

![image](https://github.com/feel0729/ExecSqlApp/blob/main/view.JPG)

GUI工具包 : Swing

執行sql的方法: 
java.sql.Statement.executeLargeUpdate(String sql);

使用的JdbcDriver:  
oracle.jdbc.driver.OracleDriver 

Reference Libraries:  
json-20220320.jar 
ojdbc8.jar  
log4j-api-2.17.0.jar  
log4j-core-2.17.0.jar 

設計發想: 
專案一開始的時候只有一個環境，手動執行語法還扛的過去，但是當專案到了中後期，Dev、SIT、UAT環境，還有分備份環境，這時要手動連線到每個資料庫執行語法就太麻煩，而且一有疏忽就要做資料庫差異比對才能處理好。

缺點: 
目前很粗略的判斷檔案內有PROCEDURE或是FUNCTION的字串，就整個檔一次執行，否則，每遇到一次分號(;)，就切為一個sql語句並執行。

未來有空想再增加的功能:  
對於Update、insert之類，會影響到資料的語法做阻擋，但目前應用的專案尚不需要。
