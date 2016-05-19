package com.csci342.justin.csci342_assignment2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class MainActivity extends AppCompatActivity {

    int     button_pressed      = 0xff969696;
    int     button_unpressed    = 0xffC8C8C8;
    String  selected_format     = "HTML";
    String  currentURL          = "https:my.uowdubai.ac.ae/restful/subject/list/";
    int     useAlternateURL     = 0; //0 = assignment, 1 = preset, 2 = custom

    ArrayList<HashMap<String,String>> all_subjects = new ArrayList<HashMap<String,String>>();
    SQLiteDatabase local_subjects;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wipeDatabase();

        setButtonPressed("HTML");
        setURLButtonPressed("assignment");
        initialiseDatabase();

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);


    }

    public void wipeDatabase()
    {
        if((local_subjects == null) || !local_subjects.isOpen()) {
            local_subjects = openOrCreateDatabase("localSubjects", SQLiteDatabase.CREATE_IF_NECESSARY, null);
        }
        try {
            local_subjects.beginTransaction();
            local_subjects.execSQL("DROP TABLE IF EXISTS Subjects");
            local_subjects.setTransactionSuccessful();
        }finally
        {
            local_subjects.endTransaction();
            local_subjects.close();
        }
    }

    public void initialiseDatabase()
    {
        try {
            if ((local_subjects == null) || !(local_subjects.isOpen())) {
                local_subjects = openOrCreateDatabase("localSubjects", SQLiteDatabase.CREATE_IF_NECESSARY, null);
            }
            boolean table_exists = doesTableExist("Subjects");
            if (!table_exists) {
                String stmt = "CREATE TABLE Subjects(Code TEXT, Description TEXT)";
                local_subjects.execSQL(stmt);
            }
        }finally {
            local_subjects.close();
        }
    }

    public boolean doesSubjectExistLocally(String subject) {
        boolean found = true;

        try {
            if ((local_subjects == null) || !(local_subjects.isOpen())) {
                local_subjects = openOrCreateDatabase("localSubjects", SQLiteDatabase.CREATE_IF_NECESSARY, null);
            }

            String[] columns = {"Code"};
            String[] search = {subject};
            Cursor checker = local_subjects.query("Subjects", columns, "Code=?", search, null, null, null);
            if (checker == null) {
                found = false;
            } else {
                checker.moveToNext();
                int column = checker.getColumnIndex("Code");
                if(checker.getCount() > 0) {
                    String check = checker.getString(column);
                    if (check.equals(subject)) {
                        found = true;
                    } else {
                        found = false;
                    }
                }
                else
                {
                    found = false;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            local_subjects.close();
            return found;
        }
    }

    public void createLocalSubject(String subject_code)
    {
        try {
            if ((local_subjects == null) || !(local_subjects.isOpen())) {
                local_subjects = openOrCreateDatabase("localSubjects", SQLiteDatabase.CREATE_IF_NECESSARY, null);
            }
            local_subjects.beginTransaction();

            ContentValues values = new ContentValues();
            values.put("Code",subject_code);
            values.put("Description","Default Description");
            local_subjects.insert("Subjects","",values);

            local_subjects.setTransactionSuccessful();
        }finally {
            local_subjects.endTransaction();
            local_subjects.close();
        }
    }

    public boolean doesTableExist(String table_name)
    {
        if(table_name.equals("Subjects"))
        {
            Cursor cursor = local_subjects.rawQuery("select DISTINCT tbl_name from sqlite_master where tbl_name = '" + table_name + "'", null);
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    cursor.close();
                    return true;
                }
            }
            cursor.close();
            return false;
        }
        return true;
    }

    public void appendLocalDatabaseToArrayList() //for display all subjects
    {
        try {
            if ((local_subjects == null) || !(local_subjects.isOpen())) {
                local_subjects = openOrCreateDatabase("localSubjects", SQLiteDatabase.CREATE_IF_NECESSARY, null);
            }
            String[] columns = {"Code","Description"};
            Cursor checker = local_subjects.query("Subjects", columns, null, null, null, null, null);
            if(checker == null)
            {
                return;
            }
            else
            {
                while(checker.moveToNext())
                {
                    int codeCol = checker.getColumnIndex("Code");
                    int descCol = checker.getColumnIndex("Description");
                    HashMap<String,String> subject = new HashMap<String,String>();
                    subject.put("Code",checker.getString(codeCol));
                    subject.put("Description",checker.getString(descCol));
                    subject.put("IsLocal?","Local");
                    all_subjects.add(subject);
                }
            }
        }finally {
            local_subjects.close();
        }
    }

    public void setButtonPressed(String x)
    {
        Button temp;
        if(!(x == null)) {
            if (x.equals("XML")) {
                temp = (Button) findViewById(R.id.MA_html_button);
                temp.setBackgroundColor(button_unpressed);
                temp = (Button) findViewById(R.id.MA_xml_button);
                temp.setBackgroundColor(button_pressed);
                temp = (Button) findViewById(R.id.MA_json_button);
                temp.setBackgroundColor(button_unpressed);
                selected_format = "XML";
            } else if (x.equals("JSON")) {
                temp = (Button) findViewById(R.id.MA_html_button);
                temp.setBackgroundColor(button_unpressed);
                temp = (Button) findViewById(R.id.MA_xml_button);
                temp.setBackgroundColor(button_unpressed);
                temp = (Button) findViewById(R.id.MA_json_button);
                temp.setBackgroundColor(button_pressed);
                selected_format = "JSON";
            } else {
                temp = (Button) findViewById(R.id.MA_html_button);
                temp.setBackgroundColor(button_pressed);
                temp = (Button) findViewById(R.id.MA_xml_button);
                temp.setBackgroundColor(button_unpressed);
                temp = (Button) findViewById(R.id.MA_json_button);
                temp.setBackgroundColor(button_unpressed);
                selected_format = "HTML";
            }
        }
        else
        {
            temp = (Button) findViewById(R.id.MA_html_button);
            temp.setBackgroundColor(button_pressed);
            temp = (Button) findViewById(R.id.MA_xml_button);
            temp.setBackgroundColor(button_unpressed);
            temp = (Button) findViewById(R.id.MA_json_button);
            temp.setBackgroundColor(button_unpressed);
            selected_format = "HTML";
        }
    }

    public void setURLButtonPressed(String x)
    {
        Button temp;
        EditText urlTextBox = (EditText) findViewById(R.id.MA_currentURL_edittext);
        if(!(x == null)) {
            if (x.equals("assignment"))
            {
                temp = (Button) findViewById(R.id.MA_URLType_Assignment_button);
                temp.setBackgroundColor(button_pressed);
                temp = (Button) findViewById(R.id.MA_URLType_Preset_button);
                temp.setBackgroundColor(button_unpressed);
                temp = (Button) findViewById(R.id.MA_URLType_Custom_Button);
                temp.setBackgroundColor(button_unpressed);
                urlTextBox.setText("https://my.uowdubai.ac.ae/restful/subject/list/");
                urlTextBox.setEnabled(false);
                useAlternateURL = 0;

            }
            else if (x.equals("preset"))
            {
                temp = (Button) findViewById(R.id.MA_URLType_Assignment_button);
                temp.setBackgroundColor(button_unpressed);
                temp = (Button) findViewById(R.id.MA_URLType_Preset_button);
                temp.setBackgroundColor(button_pressed);
                temp = (Button) findViewById(R.id.MA_URLType_Custom_Button);
                temp.setBackgroundColor(button_unpressed);
                if ( selected_format == "HTML" ) urlTextBox.setText("http://www.google.com/");
                if ( selected_format == "XML"  ) urlTextBox.setText("http://api.androidhive.info/pizza/?format=xml");
                if ( selected_format == "JSON" ) urlTextBox.setText("http://demo.codeofaninja.com/tutorials/json-example-with-php/index.php");

                urlTextBox.setEnabled(false);
                useAlternateURL = 1;
            }
            else
            {
                temp = (Button) findViewById(R.id.MA_URLType_Assignment_button);
                temp.setBackgroundColor(button_unpressed);
                temp = (Button) findViewById(R.id.MA_URLType_Preset_button);
                temp.setBackgroundColor(button_unpressed);
                temp = (Button) findViewById(R.id.MA_URLType_Custom_Button);
                temp.setBackgroundColor(button_pressed);
                urlTextBox.setText("");
                urlTextBox.setHint("Type URL Here");
                urlTextBox.setEnabled(true);
                useAlternateURL = 2;
            }
        }
        else
        {
            temp = (Button) findViewById(R.id.MA_html_button);
            temp.setBackgroundColor(button_pressed);
            temp = (Button) findViewById(R.id.MA_xml_button);
            temp.setBackgroundColor(button_unpressed);
            temp = (Button) findViewById(R.id.MA_json_button);
            temp.setBackgroundColor(button_unpressed);
            selected_format = "HTML";
        }
    }

    public String[] querySubject(String subject)
    {
        String[] standard = {"",""};

        try {
            if ((local_subjects == null) || !(local_subjects.isOpen())) {
                local_subjects = openOrCreateDatabase("localSubjects", SQLiteDatabase.CREATE_IF_NECESSARY, null);
            }

            String[] columns = {"Code","Description"};
            String[] search = {subject};
            Cursor checker = local_subjects.query("Subjects", columns, "Code=?", search, null, null, null);
            if (!(checker == null)) {
                checker.moveToNext();
                int codeCol = checker.getColumnIndex("Code");
                int descCol = checker.getColumnIndex("Description");
                if (checker.getCount() > 0) {
                    standard[0]+= checker.getString(codeCol);
                    standard[1]+= checker.getString(descCol);
                    Log.i("Returning array", "of subject");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            local_subjects.close();
            return standard;
        }
    }

    public void searchAndDisplayHTML(String subject_code)
    {
        //perform search with the URL correctly
        //check results for existence (probably description.equals("") or null for a negative check)
        //if(valid), display result
        //else, query local database
            //if(local_valid), display result
        boolean local_valid = doesSubjectExistLocally(subject_code);
        if(local_valid)
        {
            //display result in layout fields
            String[] subj = querySubject(subject_code);
            if(!(subj == null)) {
                TextView display = (TextView) findViewById(R.id.MA_subjectdisplay_textview);
                display.setText(subj[0]);
                display = (TextView) findViewById(R.id.MA_descriptiondisplay_textview);
                display.setText(subj[1]);
            }
            else
            {
                Log.i("RETURNED", "NULL");
            }
        }
        else {    //else
            //insert to local database
            createLocalSubject(subject_code);
            Toast.makeText(this,"Subject Inserted Into Database",Toast.LENGTH_LONG).show();
        }


    }

    public void searchAndDisplayXML(String subject_code)
    {
        //perform search with the URL correctly
        //check results for existence (probably description.equals("") or null for a negative check)
        //if(valid), display result
        //else, query local database
        //if(local_valid), display result
        boolean local_valid = doesSubjectExistLocally(subject_code);
        if(local_valid)
        {
            //display result in layout fields
            String[] subj = querySubject(subject_code);
            if(!(subj == null)) {
                TextView display = (TextView) findViewById(R.id.MA_subjectdisplay_textview);
                display.setText(subj[0]);
                display = (TextView) findViewById(R.id.MA_descriptiondisplay_textview);
                display.setText(subj[1]);
            }
            else
            {
                Log.i("RETURNED","NULL");
            }
        }
        else {    //else
            //insert to local database
            createLocalSubject(subject_code);
            Toast.makeText(this,"Subject Inserted Into Database",Toast.LENGTH_LONG).show();
        }


    }

    public void searchAndDisplayJSON(String subject_code)
    {
        //perform search with the URL correctly
        //check results for existence (probably description.equals("") or null for a negative check)
        //if(valid), display result
        //else, query local database
        //if(local_valid), display result
        boolean local_valid = doesSubjectExistLocally(subject_code);
        if(local_valid)
        {
            //display result in layout fields
            String[] subj = querySubject(subject_code);
            if(!(subj == null)) {
                TextView display = (TextView) findViewById(R.id.MA_subjectdisplay_textview);
                display.setText(subj[0]);
                display = (TextView) findViewById(R.id.MA_descriptiondisplay_textview);
                display.setText(subj[1]);
            }
            else
            {
                Log.i("RETURNED", "NULL");
            }
        }
        else {    //else
            //insert to local database
            createLocalSubject(subject_code);
            Toast.makeText(this,"Subject Inserted Into Database",Toast.LENGTH_LONG).show();
        }

    }

    public void SearchButtonPressed(View v)
    {
        ListView my_list = (ListView) findViewById(R.id.MA_listofsubjects_listview);
        my_list.setVisibility(View.INVISIBLE);
        LinearLayout unformatted = (LinearLayout) findViewById(R.id.MA_unformatedWebServiceRequest_linearLayout);
        unformatted.setVisibility(View.INVISIBLE);
        ScrollView unformattedSV = (ScrollView) findViewById(R.id.MA_UnformattedData_scrollview);
        unformattedSV.setVisibility(View.INVISIBLE);

        EditText temp = (EditText) findViewById(R.id.MA_searchvalue_edittext);
        String subject_searched = temp.getText().toString();
        temp.setText("");

        LinearLayout single = (LinearLayout) findViewById(R.id.MA_singlesubjectlayout_linearlayout);
        single.setVisibility(View.VISIBLE);

        if(selected_format.equals("XML"))
        {
            searchAndDisplayXML(subject_searched);
        }
        else if(selected_format.equals("JSON"))
        {
            searchAndDisplayJSON(subject_searched);
        }
        else
        {
            searchAndDisplayHTML(subject_searched);
        }


    }

    public void DisplayAllSubjectsButtonPressed(View v)
    {
        LinearLayout single = (LinearLayout) findViewById(R.id.MA_singlesubjectlayout_linearlayout);
        single.setVisibility(View.INVISIBLE);
        LinearLayout unformatted = (LinearLayout) findViewById(R.id.MA_unformatedWebServiceRequest_linearLayout);
        unformatted.setVisibility(View.INVISIBLE);
        ScrollView unformattedSV = (ScrollView) findViewById(R.id.MA_UnformattedData_scrollview);
        unformattedSV.setVisibility(View.INVISIBLE);

        Toast.makeText(this,selected_format,Toast.LENGTH_LONG).show();

        all_subjects = new ArrayList<HashMap<String,String>>();
        if(selected_format.equals("XML")) {
            //query the online database
            //create HashMap<String,String> type, populate it, and add to all_subjects
                //see appendLocalDatabaseToArrayList for code to do this
            appendLocalDatabaseToArrayList();

            ListView my_list = (ListView) findViewById(R.id.MA_listofsubjects_listview);

            SubjectListAdapter adapter = new SubjectListAdapter(this,all_subjects);

            my_list.setAdapter(adapter);

            my_list.setVisibility(View.VISIBLE);
        }
        else if(selected_format.equals("JSON"))
        {
            //query the online database
            //create HashMap<String,String> type, populate it, and add to all_subjects
            //see appendLocalDatabaseToArrayList for code to do this
            appendLocalDatabaseToArrayList();

            ListView my_list = (ListView) findViewById(R.id.MA_listofsubjects_listview);

            SubjectListAdapter adapter = new SubjectListAdapter(this,all_subjects);

            my_list.setAdapter(adapter);

            my_list.setVisibility(View.VISIBLE);
        }
        else //html
        {
            //query the online database
            //create HashMap<String,String> type, populate it, and add to all_subjects
            //see appendLocalDatabaseToArrayList for code to do this
            appendLocalDatabaseToArrayList();

            ListView my_list = (ListView) findViewById(R.id.MA_listofsubjects_listview);

            SubjectListAdapter adapter = new SubjectListAdapter(this,all_subjects);

            my_list.setAdapter(adapter);

            my_list.setVisibility(View.VISIBLE);
        }
    }

    public void HTMLButtonPressed(View v)
    {
        //Toast.makeText(this, "HTML Pressed", Toast.LENGTH_LONG).show();
        setButtonPressed("HTML");
        if ( useAlternateURL == 1 ) setURLButtonPressed("preset");

        LinearLayout single = (LinearLayout) findViewById(R.id.MA_singlesubjectlayout_linearlayout);
        single.setVisibility(View.INVISIBLE);
        ListView my_list = (ListView) findViewById(R.id.MA_listofsubjects_listview);
        my_list.setVisibility(View.INVISIBLE);

        LinearLayout unformatted = (LinearLayout) findViewById(R.id.MA_unformatedWebServiceRequest_linearLayout);
        unformatted.setVisibility(View.VISIBLE);
        ScrollView unformattedSV = (ScrollView) findViewById(R.id.MA_UnformattedData_scrollview);
        unformattedSV.setVisibility(View.VISIBLE);

        String RequestedData = getStuffFromWebService("HTML");

        TextView RDTV = (TextView) findViewById(R.id.MA_UnformattedServiceData_textview);
        RDTV.setText(RequestedData);

        TextView LDTV = (TextView) findViewById(R.id.MA_UnformattedLocalData_textview);
        all_subjects.clear();
        appendLocalDatabaseToArrayList();
        LDTV.setText(all_subjects.toString());
    }

    public void XMLButtonPressed(View v)
    {
        //Toast.makeText(this,"XML Pressed",Toast.LENGTH_LONG).show();
        setButtonPressed("XML");
        if ( useAlternateURL == 1 ) setURLButtonPressed("preset");

        LinearLayout single = (LinearLayout) findViewById(R.id.MA_singlesubjectlayout_linearlayout);
        single.setVisibility(View.INVISIBLE);
        ListView my_list = (ListView) findViewById(R.id.MA_listofsubjects_listview);
        my_list.setVisibility(View.INVISIBLE);

        LinearLayout unformatted = (LinearLayout) findViewById(R.id.MA_unformatedWebServiceRequest_linearLayout);
        unformatted.setVisibility(View.VISIBLE);
        ScrollView unformattedSV = (ScrollView) findViewById(R.id.MA_UnformattedData_scrollview);
        unformattedSV.setVisibility(View.VISIBLE);

        String RequestedData = getStuffFromWebService("XML");

        TextView RDTV = (TextView) findViewById(R.id.MA_UnformattedServiceData_textview);
        RDTV.setText(RequestedData);

        TextView LDTV = (TextView) findViewById(R.id.MA_UnformattedLocalData_textview);
        all_subjects.clear();
        appendLocalDatabaseToArrayList();
        LDTV.setText(all_subjects.toString());
    }

    public void JSONButtonPressed(View v)
    {
        //Toast.makeText(this,"JSON Pressed",Toast.LENGTH_LONG).show();
        setButtonPressed("JSON");
        if ( useAlternateURL == 1 ) setURLButtonPressed("preset");

        LinearLayout single = (LinearLayout) findViewById(R.id.MA_singlesubjectlayout_linearlayout);
        single.setVisibility(View.INVISIBLE);
        ListView my_list = (ListView) findViewById(R.id.MA_listofsubjects_listview);
        my_list.setVisibility(View.INVISIBLE);

        LinearLayout unformatted = (LinearLayout) findViewById(R.id.MA_unformatedWebServiceRequest_linearLayout);
        unformatted.setVisibility(View.VISIBLE);
        ScrollView unformattedSV = (ScrollView) findViewById(R.id.MA_UnformattedData_scrollview);
        unformattedSV.setVisibility(View.VISIBLE);

        String RequestedData = getStuffFromWebService("JSON");

        TextView RDTV = (TextView) findViewById(R.id.MA_UnformattedServiceData_textview);
        RDTV.setText(RequestedData);

        TextView LDTV = (TextView) findViewById(R.id.MA_UnformattedLocalData_textview);
        all_subjects.clear();
        appendLocalDatabaseToArrayList();
        LDTV.setText(all_subjects.toString());

    }

    public void URL_AssignmentButtonPressed(View V)
    {
        setURLButtonPressed("assignment");
    }

    public void URL_PresetButtonPressed(View V)
    {
        setURLButtonPressed("preset");
    }

    public void URL_CustomButtonPressed(View V)
    {
        setURLButtonPressed("custom");
    }

    private class SubjectListAdapter extends BaseAdapter implements View.OnClickListener {

        private Activity activity;
        private ArrayList<HashMap<String, String>> data;
        private LayoutInflater inflater=null;
        Context ctx;

        public SubjectListAdapter(Activity a, ArrayList<HashMap<String, String>> d) {
            ctx = a.getBaseContext();
            activity = a;
            data=d;
            inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public int getCount()
        {
            return data.size();
        }

        public Object getItem(int position)
        {
            return position;
        }

        public long getItemId(int position)
        {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;

            if(convertView==null)
                v = inflater.inflate(R.layout.subject_layout, null);

            TextView code = (TextView) v.findViewById(R.id.SL_codedisplay_textview);
            TextView description = (TextView) v.findViewById(R.id.SL_descdisplay_textview);
            HashMap<String, String> category = new HashMap<String, String>();
            category = data.get(position);
            code.setText(category.get("Code"));
            description.setText(category.get("Description"));
            String local = category.get("IsLocal?");
            if(local.equals("Local"))
            {
                code.setTextColor(0xfff00000);
                description.setTextColor(0xfff00000);
            }

            return v;
        }

        @Override
        public void onClick(View v) {

        }
    }

    public String getStuffFromWebService(String format)
    {
        trustManagerThingy();

        InputStream is = null;
        int len = 1000;
        String theReturnedString = "";

        try
        {
            EditText urlText = (EditText) findViewById(R.id.MA_currentURL_edittext);
            String theURL = urlText.getText().toString();//https:my.uowdubai.ac.ae/restful/subject/list/";
            //Toast.makeText(this,"URL: " + theURL,Toast.LENGTH_SHORT).show();


            URL url = new URL(theURL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            if ( theURL.substring(0,4).equals("https")) conn = (HttpsURLConnection) url.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");

            if (format.equals("HTML"))  conn.setRequestProperty("Content-Type", "application/html");
            if (format.equals("XML") )  conn.setRequestProperty("Content-Type", "application/xml");
            if (format.equals("JSON"))  conn.setRequestProperty("Content-Type", "application/json");

            conn.connect();
            //Toast.makeText(this,"Receving: Connected " + conn.toString(),Toast.LENGTH_SHORT).show();

            int resposne = conn.getResponseCode();
            //Toast.makeText(this,"Receving: Response: " + resposne,Toast.LENGTH_SHORT).show();

            is = conn.getInputStream();
            //Toast.makeText(this,"Receving: getInputStream: " + is.toString(),Toast.LENGTH_SHORT).show();

            Reader reader = new InputStreamReader(is, "UTF-8");
            char[] buffer = new char[len];
            reader.read(buffer);
            theReturnedString = new String(buffer);

            //Toast.makeText(this,"Receving: Read: " + theReturnedString.length() + " chars",Toast.LENGTH_SHORT).show();

            if ( is != null ) is.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Toast.makeText(this,"INVALID URL",Toast.LENGTH_LONG).show();

        }

        return theReturnedString;
    }

    private void trustManagerThingy()
    {
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
        };

// Install the all-trusting trust manager
        try
        {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
