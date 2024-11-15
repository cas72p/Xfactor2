package com.www.xfactor;

import android.os.StrictMode;
import android.util.Log;

import java.sql.Connection;
import java.sql.DriverManager;

public class ConnectionHelper {

    Connection con;
    String uname, pass, ip, port, database;

    public Connection connectionclass() {
        // Replace with your AWS RDS endpoint
        ip = "x-factor.cnmyeqgwshz0.us-east-2.rds.amazonaws.com";
        database = "Xfactor";
        uname = "admin";
        pass = "X-Factor";
        port = "3306";

        // Allow network operations on the main thread (only for testing)
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        Connection connection = null;
        String ConnectionURL;

        try {
            // Load MySQL JDBC driver
            Class.forName("org.mariadb.jdbc.Driver");


            // MySQL connection URL
            ConnectionURL = "jdbc:mysql://" + ip + ":" + port + "/" + database + "?user=" + uname + "&password=" + pass + "&useSSL=false";

            // Establish the connection
            connection = DriverManager.getConnection(ConnectionURL);
        } catch (Exception ex) {
            Log.e("Error", ex.getMessage());
        }

        return connection;
    }
}
