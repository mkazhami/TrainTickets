package com.mikhailkazhamiaka.viatickets;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Base64;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.client.util.StringUtils;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.*;
import com.google.api.services.gmail.model.ListMessagesResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 3;
    static final int OPEN_PICTURE_FILE = 1004;
    static final int UPDATE_FRAGMENT = 1005;
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = { GmailScopes.GMAIL_LABELS, GmailScopes.GMAIL_COMPOSE,
            GmailScopes.GMAIL_INSERT, GmailScopes.GMAIL_MODIFY, GmailScopes.GMAIL_READONLY, GmailScopes.MAIL_GOOGLE_COM };


    ProgressDialog mProgress;
    private TextView mOutputText;
    GoogleAccountCredential mCredential;
    int progressCount = 0;

    Button mUpdateTodayButton;
    Button mUpdateShowAllButton;
    Button mUpdateButton;
    String lowerEmailRange = null;
    String upperEmailRange = null;
    String lowerTicketRange = null;
    String upperTicketRange = null;
    boolean downloadAll = false;


    ArrayList<Ticket> tickets;
    TicketsListAdapter listAdapter;
    DBAdapter dbAdapter;
    private ConcurrentLinkedQueue<Pair<String, String>> newFiles; // first: pdf second: qr


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        dbAdapter = new DBAdapter(this);

        tickets = new ArrayList<>();
        newFiles = new ConcurrentLinkedQueue<>();
        listAdapter = new TicketsListAdapter(tickets, this);

        ListView listView = (ListView) findViewById(R.id.ticketsListView);
        listView.setAdapter(listAdapter);

        mUpdateTodayButton = (Button) findViewById(R.id.showTodayButton);
        mUpdateTodayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                Calendar c = Calendar.getInstance();
                SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
                String formattedDate = df.format(c.getTime()).replace("-", "/");

                lowerTicketRange = formattedDate;
                upperTicketRange = formattedDate;
                lowerEmailRange = null;
                upperEmailRange = null;
                dbAdapter.open();
                tickets.clear();
                listAdapter.notifyDataSetChanged();
                Cursor cursor = dbAdapter.getTicketsBetweenDates(lowerTicketRange, upperTicketRange);
                addMessages(cursor);
                dbAdapter.close();
            }
        });

        mUpdateShowAllButton = (Button) findViewById(R.id.showAllUpdateButton);
        mUpdateShowAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dbAdapter.open();
                tickets.clear();
                listAdapter.notifyDataSetChanged();
                Cursor cursor = dbAdapter.getAllTickets();
                addMessages(cursor);
                dbAdapter.close();
            }
        });
        mUpdateButton = (Button) findViewById(R.id.updateButton);
        mUpdateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UpdateFragment frag = new UpdateFragment();
                frag.show(getSupportFragmentManager(), "dialog");
            }
        });

        mProgress = new ProgressDialog(this);
        mProgress.setCancelable(false);
        mProgress.setMessage("Fetching tickets ...");

        mOutputText = (TextView) findViewById(R.id.textView);
        mOutputText.setMovementMethod(new ScrollingMovementMethod());

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // get READ_CONTACTS permission explicitly - required to get/set account name
        // not mentioned in google documentation for whatever reason
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CONTACTS},
                    MY_PERMISSIONS_REQUEST_READ_CONTACTS);
        }


        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff())
                .setSelectedAccountName(settings.getString(PREF_ACCOUNT_NAME, null));

        dbAdapter.open();
        Cursor cursor = dbAdapter.getAllTickets();
        addMessages(cursor);

        dbAdapter.close();
        //refreshResults(); // TODO: REMOVE AND ONLY UPDATE ON REQUEST
    }

    public void onUpdateFragmentReturn(Bundle bundle) {
        lowerEmailRange = bundle.getString("email_lower");
        upperEmailRange = bundle.getString("email_upper");
        lowerTicketRange = bundle.getString("ticket_lower");
        upperTicketRange = bundle.getString("ticket_upper");
        boolean clearDB = bundle.getBoolean("clear_db");
        downloadAll = bundle.getBoolean("download_all");
        if (lowerTicketRange != null && upperTicketRange != null) {
            tickets.clear();
            listAdapter.notifyDataSetChanged();
        } else if (clearDB || downloadAll) {
            dbAdapter.open();
            dbAdapter.clearAllData();
            dbAdapter.close();
            tickets.clear();
        }
        if ((lowerEmailRange != null && upperEmailRange != null) || downloadAll) {
            refreshResults();
        }
        mProgress.setMessage("Updating database...");
        mProgress.show();
        dbAdapter.open();
        Cursor cursor = null;
        if (lowerTicketRange != null && upperTicketRange != null && lowerTicketRange.length() > 0 && upperTicketRange.length() > 0) {
            cursor = dbAdapter.getTicketsBetweenDates(lowerTicketRange, upperTicketRange);
        } else {
            cursor = dbAdapter.getAllTickets();
        }

        addMessages(cursor);

        dbAdapter.close();
        mProgress.hide();
    }

    private void processMessages() {
        listAdapter.notifyDataSetChanged();
    }

    private void addMessages(Cursor cursor) {
        while (cursor.moveToNext()) {
            Ticket ticket = new Ticket();
            ticket.setOrigin(cursor.getString(cursor.getColumnIndex(DBAdapter.TicketsEntry.COLUMN_NAME_ORIGIN)));
            ticket.setDestination(cursor.getString(cursor.getColumnIndex(DBAdapter.TicketsEntry.COLUMN_NAME_DESTINATION)));
            ticket.setDate(cursor.getString(cursor.getColumnIndex(DBAdapter.TicketsEntry.COLUMN_NAME_DATE)));
            ticket.setTime(cursor.getString(cursor.getColumnIndex(DBAdapter.TicketsEntry.COLUMN_NAME_TIME)));
            ticket.setTrainNumber(cursor.getString(cursor.getColumnIndex(DBAdapter.TicketsEntry.COLUMN_NAME_TRAIN_NUMBER)));
            ticket.setCar(cursor.getString(cursor.getColumnIndex(DBAdapter.TicketsEntry.COLUMN_NAME_CAR)));
            ticket.setSeat(cursor.getString(cursor.getColumnIndex(DBAdapter.TicketsEntry.COLUMN_NAME_SEAT)));
            ticket.setPdfFile(cursor.getString(cursor.getColumnIndex(DBAdapter.TicketsEntry.COLUMN_NAME_PDF_FILE)));
            ticket.setQrFile(cursor.getString(cursor.getColumnIndex(DBAdapter.TicketsEntry.COLUMN_NAME_QR_FILE)));
            tickets.add(ticket);
            listAdapter.notifyDataSetChanged();
        }
        cursor.close();
    }

    /**
     * Called whenever this activity is pushed to the foreground, such as after
     * a call to onCreate().
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (isGooglePlayServicesAvailable()) {
            //refreshResults();
        } else {
            mOutputText.setText("Google Play Services required: " +
                    "after installing, close and relaunch this app.");
        }

        /*if (!picFiles.isEmpty()) {
            String fileStr = picFiles.remove();
            File file = new File(getBaseContext().getFilesDir().getAbsolutePath() + "/" + fileStr);
            Intent intent = new Intent();
            intent.setAction(android.content.Intent.ACTION_VIEW);
            Uri uri = Uri.parse("content://com.mikhailkazhamiaka.viatickets/" + fileStr);
            intent.setDataAndType(uri, "image/*");
            startActivityForResult(intent, OPEN_PICTURE_FILE);
        } */
    }

    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode code indicating the result of the incoming
     *     activity result.
     * @param data Intent (containing result data) returned by incoming
     *     activity result.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    isGooglePlayServicesAvailable();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        mCredential.setSelectedAccountName(accountName);
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                    }
                } else if (resultCode == RESULT_CANCELED) {
                    mOutputText.setText("Account unspecified.");
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode != RESULT_OK) {
                    chooseAccount();
                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_CONTACTS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted

                } else {
                    mOutputText.setText("Failed to acquire permissions."); // TODO: fix this case
                }
                return;
            }
        }
    }

    /**
     * Attempt to get a set of data from the Gmail API to display. If the
     * email address isn't known yet, then call chooseAccount() method so the
     * user can pick an account.
     */
    private void refreshResults() {
        if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else {
            if (isDeviceOnline()) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                new MakeRequestTask(mCredential).execute();
            } else {
                mOutputText.setText("No network connection available.");
            }
        }
    }

    /**
     * Starts an activity in Google Play Services so the user can pick an
     * account.
     */
    private void chooseAccount() {
        startActivityForResult(mCredential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
    }

    /**
     * Checks whether the device currently has a network connection.
     * @return true if the device has a network connection, false otherwise.
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Check that Google Play services APK is installed and up to date. Will
     * launch an error dialog for the user to update Google Play Services if
     * possible.
     * @return true if Google Play Services is available and up to
     *     date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        final int connectionStatusCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (GooglePlayServicesUtil.isUserRecoverableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
            return false;
        } else if (connectionStatusCode != ConnectionResult.SUCCESS ) {
            return false;
        }
        return true;
    }

    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     * @param connectionStatusCode code describing the presence (or lack of)
     *     Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(final int connectionStatusCode) {
        Dialog dialog = GooglePlayServicesUtil.getErrorDialog(
                connectionStatusCode,
                MainActivity.this,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }



    /**
     * An asynchronous task that handles the Gmail API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class MakeRequestTask extends AsyncTask<Void, Void, List<String>> {
        private com.google.api.services.gmail.Gmail mService = null;
        private Exception mLastError = null;

        public MakeRequestTask(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.gmail.Gmail.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("VIA Tickets")
                    .build();
        }

        /**
         * Background task to call Gmail API.
         * @param params no parameters needed for this task.
         */
        @Override
        protected List<String> doInBackground(Void... params) {
            try {
                if(android.os.Debug.isDebuggerConnected())
                    android.os.Debug.waitForDebugger();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mProgress.setMessage("Calling Gmail API ...");
                        mProgress.show();
                    }
                });
                return getDataFromApi();
            } catch (Exception e) {
                mLastError = e;
                Log.i("THREAD ERROR", e.toString());
                cancel(true);
                return null;
            }
        }

        /**
         * Fetch a list of Gmail messages attached to the specified account with keyword 'VIA'.
         * @return List of Strings labels.
         * @throws IOException
         */
        private List<String> getDataFromApi() throws IOException {
            // Get the labels in the user's account.
            String user = "me";
            List<Message> messages = new ArrayList<Message>();
            String constraints = "from:service@viarail.ca";
            if (lowerEmailRange != null && upperEmailRange != null
                    && lowerEmailRange.length() > 0 && upperEmailRange.length() > 0
                    && !downloadAll) {
                String split1[] = lowerEmailRange.split("/");
                String split2[] = upperEmailRange.split("/");
                lowerEmailRange = split1[2] + "/" + split1[1] + "/" + split1[0];
                upperEmailRange = split2[2] + "/" + split2[1] + "/" + split2[0];
                constraints += " after:" + lowerEmailRange + " before:" + upperEmailRange;
            }
            Gmail.Users.Messages.List request = mService.users().messages().list("me")
                    //.setQ("from:service@viarail.ca after:2016/01/01 before:2016/02/24");
                    .setQ(constraints);

            ListMessagesResponse response = null;
            int count = 0;
            do {
                response = request.execute();
                if (response.size() > 1)
                    messages.addAll(response.getMessages());
                else
                    break;
                request.setPageToken(response.getNextPageToken());

            } while (request.getPageToken() != null && request.getPageToken().length() > 0 /*&& (++count) < 3*/); // TODO: REMOVE LIMIT COUNT

            List<String> messageIds = new ArrayList<>();
            int c = 0;
            for (Message message : messages) {
                messageIds.add(message.getId());
                //if (++c > 2)
                //    break; // TODO: REMOVE
            }

            List<String> messageBodies = new ArrayList<>();
            String pic = "";
            String pdf = "";
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mProgress.setMessage("Downloading emails and attachments...");
                }
            });

            for (String id : messageIds) {
                messageBodies.add(StringUtils.newStringUtf8(Base64.decodeBase64(
                        mService.users().messages().get("me", id).setFormat("full").execute()
                                .getPayload().getBody().getData())));

                Message message = mService.users().messages().get("me", id).execute();
                List<MessagePart> parts = message.getPayload().getParts();
                List<String> fileNames = new ArrayList<>();
                if (parts != null) {
                    // TODO: Generalize to multiple tickets
                    // although shouldn't have more than 2 tickets (round trip)
                    // problem exists with keeping pdf and qr files paired
                    String pdfName1 = "";
                    String pdfName2 = "";
                    String qrName1 = "";
                    String qrName2 = "";
                    for (MessagePart part : parts) {
                        if (part.getFilename() != null && part.getFilename().length() > 0) {
                            String filename = part.getFilename();
                            String attId = part.getBody().getAttachmentId();
                            MessagePartBody attachPart = mService.users().messages().attachments().
                                    get("me", id, attId).execute();
                            byte[] fileByteArray = Base64.decodeBase64(attachPart.getData());
                            FileOutputStream fileOutFile = openFileOutput(filename, Context.MODE_PRIVATE);
                            fileOutFile.write(fileByteArray);
                            fileOutFile.close();
                            fileNames.add(filename);
                            if (filename.contains("pdf") && pdfName1.equals("")) pdfName1 = filename;
                            else if (filename.contains("pdf")) pdfName2 = filename;
                            else if (filename.contains("jpg") && qrName1.equals("")) qrName1 = filename;
                            else if (filename.contains("jpg")) qrName2 = filename;
                        }
                    }
                    assert(!pdfName1.equals("") && !qrName1.equals(""));
                    newFiles.add(new Pair<String, String>(pdfName1, qrName1));
                    if (!pdfName2.equals("") && !qrName2.equals("")) {
                        newFiles.add(new Pair<String, String>(pdfName2, qrName2));
                    }
                }
            }
            Log.i("RUNNABLE", "Finished Gmail API calls. Starting e-mail parsing...");
            final int size = newFiles.size();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressCount = 0;
                    mProgress.setMessage("Parsing emails... " + Integer.toString(progressCount) + " out of " + Integer.toString(size));
                }
            });
            dbAdapter.open();
            while(!newFiles.isEmpty()) {
                Pair<String, String> files = newFiles.remove();
                String path = getBaseContext().getFilesDir() + "/";
                Ticket ticket = PDFParse.parsePDF(/*path + */files.first, getBaseContext());
                ticket.setQrFile(/*path + */files.second);
                tickets.add(ticket); // TODO: insert in sorted order
                dbAdapter.insertTicket(ticket);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressCount++;
                        mProgress.setMessage("Parsing emails... " + Integer.toString(progressCount) + " out of " + Integer.toString(size));
                        processMessages();
                    }
                });
            }
            dbAdapter.close();
            Log.i("RUNNABLE", "Finished e-mail parsing");

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    processMessages();
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
            });
            return messageBodies;
        }


        @Override
        protected void onPreExecute() {
            mOutputText.setText("");
            mProgress.show();
        }

        @Override
        protected void onPostExecute(List<String> output) {
            mProgress.hide();
            if (output == null || output.size() == 0) {
                mOutputText.setText("No results returned.");
            } else {
                output.add(0, "Data retrieved using the Gmail API:");
                mOutputText.setText(TextUtils.join("\n", output));
            }
        }

        @Override
        protected void onCancelled() {
            mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            MainActivity.REQUEST_AUTHORIZATION);
                } else {
                    mOutputText.setText("The following error occurred:\n"
                            + mLastError.getMessage());
                }
            } else {
                mOutputText.setText("Request cancelled.");
            }
        }
    }
}






