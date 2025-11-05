package com.example.dockyardworkerrecord;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    EditText et_pno, et_name, et_rank, et_workshop, et_contact, et_place, et_hoist, et_card_exit;
    Button btnAdd, btnMarkExit, btnExport;
    TextView tvMessage;
    DBHelper db;
    private static final int REQ_PERM = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new DBHelper(this);

        et_pno = findViewById(R.id.et_pno);
        et_name = findViewById(R.id.et_name);
        et_rank = findViewById(R.id.et_rank);
        et_workshop = findViewById(R.id.et_workshop);
        et_contact = findViewById(R.id.et_contact);
        et_place = findViewById(R.id.et_place);
        et_hoist = findViewById(R.id.et_hoist);
        et_card_exit = findViewById(R.id.et_card_exit);

        btnAdd = findViewById(R.id.btn_add_entry);
        btnMarkExit = findViewById(R.id.btn_mark_exit);
        btnExport = findViewById(R.id.btn_export);
        tvMessage = findViewById(R.id.tv_message);

        btnAdd.setOnClickListener(v -> addEntry());
        btnMarkExit.setOnClickListener(v -> markExit());
        btnExport.setOnClickListener(v -> exportToday());

        checkPermissions();
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQ_PERM);
        }
    }

    private void addEntry() {
        String pno = et_pno.getText().toString().trim();
        String name = et_name.getText().toString().trim();
        String rank = et_rank.getText().toString().trim();
        String workshop = et_workshop.getText().toString().trim();
        String contact = et_contact.getText().toString().trim();
        String place = et_place.getText().toString().trim();
        String hoist = et_hoist.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter name", Toast.LENGTH_SHORT).show();
            return;
        }

        String cardno = generateCardNumber();
        String entryTime = now();
        String date = todayDate();

        ContentValues cv = new ContentValues();
        cv.put("pno", pno);
        cv.put("name", name);
        cv.put("rank", rank);
        cv.put("workshop", workshop);
        cv.put("contact", contact);
        cv.put("place", place);
        cv.put("hoist", hoist);
        cv.put("cardno", cardno);
        cv.put("entry_time", entryTime);
        cv.put("exit_time", (String) null);
        cv.put("date", date);

        long id = db.insertRecord(cv);
        if (id > 0) {
            tvMessage.setText("Entry added. Give card number: " + cardno);
            clearInputs();
        } else tvMessage.setText("Failed to insert");
    }

    private String generateCardNumber() {
        // simple unique code: short UUID
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void clearInputs() {
        et_pno.setText("");
        et_name.setText("");
        et_rank.setText("");
        et_workshop.setText("");
        et_contact.setText("");
        et_place.setText("");
        et_hoist.setText("");
    }

    private void markExit() {
        String card = et_card_exit.getText().toString().trim();
        if (card.isEmpty()) {
            Toast.makeText(this, "Enter card number", Toast.LENGTH_SHORT).show();
            return;
        }
        String exitTime = now();
        int updated = db.markExit(card, exitTime);
        if (updated > 0) {
            tvMessage.setText("Exit marked for card " + card + " at " + exitTime);
            et_card_exit.setText("");
        } else tvMessage.setText("No open entry found for that card (or already exited)");
    }

    private void exportToday() {
        String date = todayDate();
        Cursor c = db.getTodayRecords(date);
        if (c == null || c.getCount() == 0) {
            tvMessage.setText("No records for today: " + date);
            return;
        }

        String filename = "dockyard_" + date + ".csv"; // yyyy-MM-dd
        try {
            File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloads.exists()) downloads.mkdirs();
            File out = new File(downloads, filename);
            FileWriter fw = new FileWriter(out);

            // header
            fw.append("S.no,P.no,Name,Rank,Workshop no,Contact no,Place to visit,Hoist name,Card no,Entry time,Exit time,Date\n");

            int sno = 1;
            while (c.moveToNext()) {
                String pno = c.getString(c.getColumnIndexOrThrow("pno"));
                String name = c.getString(c.getColumnIndexOrThrow("name"));
                String rank = c.getString(c.getColumnIndexOrThrow("rank"));
                String workshop = c.getString(c.getColumnIndexOrThrow("workshop"));
                String contact = c.getString(c.getColumnIndexOrThrow("contact"));
                String place = c.getString(c.getColumnIndexOrThrow("place"));
                String hoist = c.getString(c.getColumnIndexOrThrow("hoist"));
                String cardno = c.getString(c.getColumnIndexOrThrow("cardno"));
                String entry = c.getString(c.getColumnIndexOrThrow("entry_time"));
                String exit = c.getString(c.getColumnIndexOrThrow("exit_time"));
                String d = c.getString(c.getColumnIndexOrThrow("date"));

                // escape commas simply by wrapping fields in quotes
                fw.append(String.valueOf(sno)).append(",")
                        .append(quote(pno)).append(",")
                        .append(quote(name)).append(",")
                        .append(quote(rank)).append(",")
                        .append(quote(workshop)).append(",")
                        .append(quote(contact)).append(",")
                        .append(quote(place)).append(",")
                        .append(quote(hoist)).append(",")
                        .append(quote(cardno)).append(",")
                        .append(quote(entry)).append(",")
                        .append(quote(exit)).append(",")
                        .append(quote(d)).append("\n");
                sno++;
            }
            fw.flush();
            fw.close();
            c.close();

            tvMessage.setText("Exported to Downloads/" + filename);
            Toast.makeText(this, "CSV exported: " + filename, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            tvMessage.setText("Export failed: " + e.getMessage());
        }
    }

    private String quote(String s) {
        if (s == null) return "";
        return """ + s.replace(""", """") + """; // wrap in quotes and escape quotes if any
    }

    private String now() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    private String todayDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }
}
