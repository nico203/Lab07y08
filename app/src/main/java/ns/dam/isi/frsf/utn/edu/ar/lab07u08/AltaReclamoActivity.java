package ns.dam.isi.frsf.utn.edu.ar.lab07u08;

import android.*;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.android.gms.maps.model.LatLng;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AltaReclamoActivity extends AppCompatActivity implements View.OnClickListener {

    static final int REQUEST_TAKE_PHOTO = 1;

    private EditText descripcionReclamoEditText;
    private EditText telefonoContactoEditText;
    private EditText mailContactoEditText;
    private Button reclamarButton;
    private Button cancelarButton;
    private Button adjuntarFotoButton;
    private ImageView imageView;

    private Reclamo reclamo;
    private LatLng ubicacion;

    private String mCurrentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alta_reclamo);

        descripcionReclamoEditText = (EditText) findViewById(R.id.descripcionReclamoEditText);
        telefonoContactoEditText = (EditText) findViewById(R.id.telefonoContactoEditText);
        mailContactoEditText = (EditText) findViewById(R.id.mailContactoEditText);
        reclamarButton = (Button) findViewById(R.id.reclamarButton);
        cancelarButton = (Button) findViewById(R.id.cancelarButton);
        adjuntarFotoButton = (Button) findViewById(R.id.adjuntarFotoButton);
        imageView = (ImageView) findViewById(R.id.imageView);

        reclamarButton.setOnClickListener(this);
        cancelarButton.setOnClickListener(this);
        adjuntarFotoButton.setOnClickListener(this);

        if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            adjuntarFotoButton.setVisibility(Button.GONE);
        }

        reclamo = new Reclamo();
        ubicacion = (LatLng) getIntent().getExtras().get("coordenadas");
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.adjuntarFotoButton) {
            if(requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA})) {
                dispatchTakePictureIntent();
            }
            return;
        }
        Intent ret = getIntent();
        switch (v.getId()) {
            case R.id.reclamarButton:
                reclamo.setEmail(mailContactoEditText.getText().toString());
                reclamo.setLatitud(ubicacion.latitude);
                reclamo.setLongitud(ubicacion.longitude);
                reclamo.setTelefono(telefonoContactoEditText.getText().toString());
                reclamo.setTitulo(descripcionReclamoEditText.getText().toString());

                ret.putExtra("reclamo", reclamo);
                setResult(RESULT_OK, ret);
                break;
            case R.id.cancelarButton:
                setResult(RESULT_CANCELED, ret);
                break;
        }
        finish();
    }

    private boolean requestPermissions(String[] permissions) {
        List<String> permisosRequeridos = new ArrayList<>();

        for( String permiso : permissions) {
            if(ContextCompat.checkSelfPermission(this, permiso) != PackageManager.PERMISSION_GRANTED) {
                permisosRequeridos.add(permiso);
            }
        }

        if(!permisosRequeridos.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permisosRequeridos.toArray(new String[permisosRequeridos.size()]),
                    12);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 12: {
                boolean aceptado = true;
                for(int permiso : grantResults) {
                    if(permiso != PackageManager.PERMISSION_GRANTED) {
                        aceptado = false;
                    }
                }
                if(aceptado) {
                    dispatchTakePictureIntent();
                }
                break;
            }
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e("Error camara", ex.getMessage());
            }

            // Continue only if the File was successfully created
            if (photoFile != null) {
                reclamo.setImagenPath(mCurrentPhotoPath);
                Uri photoURI = FileProvider.getUriForFile(this,
                        "ns.dam.isi.frsf.utn.edu.ar.lab07u08.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private void setPic() {
        // Get the dimensions of the View
        int targetW = imageView.getWidth();
        int targetH = imageView.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        imageView.setImageBitmap(bitmap);
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        //File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            galleryAddPic();
            setPic();
        }
    }
}
