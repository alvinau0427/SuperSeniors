package com.example.whaledidyougo.ui.dashboard;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.FragmentManager;

import com.example.whaledidyougo.R;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AddContactActivity extends AppCompatActivity {
    public EditText name, phone_no;
    public ImageButton img_button;
    public Button confirm, reset;
    public Bitmap bitmap;
    public String fileName;
    public String mFilePath;
    public String insert_name, insert_phone, insert_image;

    String currentImagePath =null;

    final String DATABASE_NAME = "elderly_care.db";
    final String TABLE_NAME = "contact_book";

    public static final String FILE_NAME = "contact_info.json"; // A json file to store 3 contact
    public final static int CAMERA_REQUEST_CODE = 1;
    public final static int GALLERY_REQUEST_CODE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_contact);

        // Create SQLite
        final SQLiteDatabase db = openOrCreateDatabase(DATABASE_NAME, Context.MODE_PRIVATE, null);
        String createTable = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "(Name VARCHAR(255), " + "Phone VARCHAR(255), " + "Image BLOB);";
        db.execSQL(createTable);

        // Get view item
        name = (EditText) findViewById(R.id.people1_name);
        phone_no = (EditText) findViewById(R.id.people1_num);
        img_button = (ImageButton) findViewById(R.id.people1_image);
        confirm = (Button) findViewById(R.id.confirm);
        reset = (Button) findViewById(R.id.reset);

        // Choose to turn on the camera or album
        img_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(AddContactActivity.this);
                myAlertDialog.setTitle(R.string.image_choice);

                myAlertDialog.setPositiveButton(R.string.image_choice_album,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface arg0, int arg1) {
                                Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                                startActivityForResult(intent,GALLERY_REQUEST_CODE);
                            }
                        });

                myAlertDialog.setNegativeButton(R.string.image_choice_camera,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface arg0, int arg1) {
                                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                                if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                                    File imageFile = null;
                                    try {
                                        imageFile = getImageFile();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                    if (imageFile != null) {
                                        Uri imageUri = FileProvider.getUriForFile(getApplicationContext(),"com.example.provider.byWhaleDidYouGo", imageFile);
                                        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                                        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
                                    }
                                }
                            }
                        });
                myAlertDialog.show();
            }
        });


        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                insert_name = name.getText().toString();
                insert_phone = phone_no.getText().toString();

                if (insert_name != null && insert_phone != null && insert_image != null) {
                    SQLiteStatement sqLiteStatement = db.compileStatement("INSERT INTO " + TABLE_NAME + " (Name, Phone, Image) VALUES (?, ?, ?)");
                    sqLiteStatement.bindString(1, insert_name);
                    sqLiteStatement.bindString(2, insert_phone);
                    sqLiteStatement.bindString(3, insert_image);
                    sqLiteStatement.execute();
                    AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(AddContactActivity.this);
                    myAlertDialog.setTitle(R.string.contact_setup_complete);
                    myAlertDialog.setPositiveButton(R.string.title_contact_confirm,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface arg0, int arg1) {
                                    //Intent i = new Intent(AddContactActivity.this, DashboardFragment.class);
                                    //startActivity(i);
                                    finish();
                                }
                            });
                    myAlertDialog.show();
                } else {
                    AlertDialog.Builder myAlertDialog = new AlertDialog.Builder(AddContactActivity.this);
                    myAlertDialog.setTitle(R.string.contact_setup_missing);
                    myAlertDialog.setPositiveButton(R.string.title_contact_confirm,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface arg0, int arg1) {

                                }
                            });
                    myAlertDialog.show();
                }
            }
        });

        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                name.setText("");
                phone_no.setText("");
                img_button.setImageResource(R.drawable.img_choose_photo);
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent arg2) {
        super.onActivityResult(requestCode, resultCode, arg2);

        // Select to take photo
        if (resultCode == RESULT_OK && requestCode == CAMERA_REQUEST_CODE) {

            Bitmap bitmap = BitmapFactory.decodeFile(currentImagePath);
            String ss = currentImagePath;

            int rotate = 0;
            try {
                ExifInterface exif = new ExifInterface(currentImagePath);
                int orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        rotate = 270;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        rotate = 180;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        rotate = 90;
                        break;
                }
            } catch (Exception e) { }

            Matrix matrix = new Matrix();
            matrix.postRotate(rotate);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
            byte[] b = baos.toByteArray();
            insert_image = Base64.encodeToString(b, Base64.DEFAULT);
            img_button.setImageBitmap(bitmap);
        } else if (resultCode == RESULT_OK && requestCode == GALLERY_REQUEST_CODE) {
            // Select to gallery
            Uri uri = arg2.getData();
            String[] filePath = {MediaStore.Images.Media.DATA};
            Cursor c = getContentResolver().query(uri, filePath, null, null, null);
            c.moveToFirst();
            int columnIndex = c.getColumnIndex(filePath[0]);
            String imagePath = c.getString(columnIndex);
            c.close();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Bitmap temp_bitmap = BitmapFactory.decodeFile(imagePath);

            //add
            int rotate = 0;
            try {
                android.media.ExifInterface exif = new android.media.ExifInterface(imagePath);
                int orirntation = exif.getAttributeInt(
                        android.media.ExifInterface.TAG_ORIENTATION, android.media.ExifInterface.ORIENTATION_NORMAL);

                switch (orirntation) {
                    case android.media.ExifInterface.ORIENTATION_ROTATE_270:
                        rotate = 270;
                        break;
                    case android.media.ExifInterface.ORIENTATION_ROTATE_180:
                        rotate = 180;
                        break;
                    case android.media.ExifInterface.ORIENTATION_ROTATE_90:
                        rotate = 90;
                        break;
                }
            }catch (Exception e) { }

            Matrix matrix = new Matrix();
            matrix.postRotate(rotate);
            temp_bitmap = Bitmap.createBitmap(temp_bitmap, 0, 0, temp_bitmap.getWidth(), temp_bitmap.getHeight(), matrix, true);
            //

            temp_bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
            byte[] b = baos.toByteArray();
            insert_image = Base64.encodeToString(b, Base64.DEFAULT);
            img_button.setImageBitmap(temp_bitmap);
        }
    }

    public File getImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageName = "jpg_"+timeStamp+"_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File imageFile = File.createTempFile(imageName, ".jpg", storageDir);
        currentImagePath = imageFile.getAbsolutePath();
        return imageFile;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
