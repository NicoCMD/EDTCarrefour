package com.example.edtcarrefour

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.app.TimePickerDialog
import android.app.TimePickerDialog.OnTimeSetListener
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.CalendarContract
import android.provider.MediaStore

import android.view.*
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition

import java.io.File
import java.io.IOException

import java.text.SimpleDateFormat
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

class MainActivity : AppCompatActivity() {

            lateinit var apercuimg: ImageView
            lateinit var listview:ListView
            lateinit var spAdapter:ArrayAdapter<String>
            lateinit var bout_sync:Button
    private lateinit var cArrayAdapter: CustomArrayAdapter
    private lateinit var sauv_preferences:SharedPreferences
                     var listeevent:MutableList<PosteEvent> = mutableListOf()

    private          var currentPhotoPath:String? = null
    private          var textReconnu:String? = null

    private          var pe:PlanningExtractor? = null
    private          var currentPhotoUri: Uri? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        apercuimg = findViewById(R.id.imgView)

        listview = findViewById(R.id.listevents)
        bout_sync = findViewById(R.id.bouton_sync)
        bout_sync.setBackgroundColor(Color.rgb(63,81,181))
        bout_sync.visibility = Button.GONE

        sauv_preferences = getPreferences(Context.MODE_PRIVATE)

        cArrayAdapter = CustomArrayAdapter(this,R.layout.listelement,R.id.eventtitretextview,listeevent,isDarkMode())
        listview.adapter = cArrayAdapter

        listview.setOnItemClickListener {parent: AdapterView<*>?, view: View?, position: Int, id: Long ->

            val eventdialog = AlertDialog.Builder(this@MainActivity)
            eventdialog.setTitle("Changer un événeement")
            val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val mydialogview: View = inflater.inflate(R.layout.dialog_changeevent, null, false)

            eventdialog.setView(mydialogview)

            val sp = mydialogview.findViewById<Spinner>(R.id.spinner)
            val datetxt = mydialogview.findViewById<TextView>(R.id.dateTxtView)
            val debuttxt = mydialogview.findViewById<TextView>(R.id.heuredebutTxtView)
            val fintxt = mydialogview.findViewById<TextView>(R.id.heurefinTxtView)
            val boutSuppr = mydialogview.findViewById<Button>(R.id.bout_suppr)
            lateinit var eventdialog_dialog:AlertDialog
            spAdapter = ArrayAdapter(applicationContext,android.R.layout.simple_list_item_1,PosteEvent.ROLES)
            sp.adapter = spAdapter
            sp.setSelection(PosteEvent.getIndexAssign(listeevent[position].assignation));

            val dateFormat = SimpleDateFormat("E dd/MM/yy", Locale.FRENCH)
            val heureFormat = SimpleDateFormat("HH:mm", Locale.FRENCH)
            var c_debut:Calendar = PlanningExtractor.copyCalendar(listeevent[position].debutEvent)
            var c_fin:Calendar = PlanningExtractor.copyCalendar(listeevent[position].finEvent)
            datetxt.setText(dateFormat.format(c_debut.time))


            val dateSetListener: OnDateSetListener
            dateSetListener = OnDateSetListener { view, year, month, dayOfMonth ->
                //c_debut = new GregorianCalendar(year,month,dayOfMonth,c_debut.get(Calendar.HOUR_OF_DAY),c_debut.get(Calendar.MINUTE));
                    c_debut[Calendar.YEAR] = year
                    c_debut[Calendar.MONTH] = month
                    c_debut[Calendar.DAY_OF_MONTH] = dayOfMonth
                    c_fin[Calendar.YEAR] = year
                    c_fin[Calendar.MONTH] = month
                    c_fin[Calendar.DAY_OF_MONTH] = dayOfMonth
                    datetxt.text = dateFormat.format(c_debut.time)
                }
            datetxt.setOnClickListener { //Configuration configuration = MainActivity.this.getResources().getConfiguration();
                Locale.setDefault(Locale.FRENCH)
                val dateDialog = DatePickerDialog(this@MainActivity,android.R.style.Theme_Holo_Light_Dialog,dateSetListener,c_debut[Calendar.YEAR],c_debut[Calendar.MONTH],c_debut[Calendar.DAY_OF_MONTH])
                dateDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                dateDialog.create()
                dateDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setBackgroundColor(Color.WHITE)
                dateDialog.getButton(DialogInterface.BUTTON_POSITIVE).setBackgroundColor(Color.WHITE)
                dateDialog.show()
            }

            debuttxt.setText(heureFormat.format(c_debut.time))
            fintxt.setText(heureFormat.format(c_fin.time))

            debuttxt.setOnClickListener {v ->
                val timePickerDialog = TimePickerDialog(this@MainActivity,OnTimeSetListener { view, hourOfDay, minute ->
                        c_debut[Calendar.HOUR_OF_DAY] = hourOfDay
                        c_debut[Calendar.MINUTE] = minute
                        debuttxt.text = heureFormat.format(c_debut.time)
                        if (c_debut.after(c_fin) || c_debut.time.compareTo(c_fin.time)==0) {
                            Toast.makeText(applicationContext,"Attention : le début est avant la fin", Toast.LENGTH_LONG).show()
                            c_fin[Calendar.HOUR_OF_DAY] = hourOfDay
                            c_fin[Calendar.MINUTE] = minute
                            fintxt.text = heureFormat.format(c_fin.time)
                        }
                    },c_debut[Calendar.HOUR_OF_DAY],c_debut[Calendar.MINUTE],true)
                timePickerDialog.show()
            }
            fintxt.setOnClickListener{v ->
                val timePickerDialog = TimePickerDialog(this@MainActivity, OnTimeSetListener { view, hourOfDay, minute ->
                    c_fin[Calendar.HOUR_OF_DAY] = hourOfDay
                    c_fin[Calendar.MINUTE] = minute
                    fintxt.text = heureFormat.format(c_fin.time)
                    if (c_fin.before(c_debut) || c_debut.time.compareTo(c_fin.time) == 0) {
                        Toast.makeText(getApplicationContext(),"Attention : la fin est après le début", Toast.LENGTH_LONG).show();
                        c_debut[Calendar.HOUR_OF_DAY] = hourOfDay
                        c_debut[Calendar.MINUTE] = minute
                        debuttxt.text=(heureFormat.format(c_debut.time))
                    }
                },c_fin[Calendar.HOUR_OF_DAY],c_fin[Calendar.MINUTE],true)
                timePickerDialog.show()
            }
            boutSuppr.setOnClickListener {  }
            boutSuppr.setOnClickListener {v ->
                AlertDialog.Builder(this@MainActivity)
                        .setMessage("Etes-vous certain de supprimer cet événement?")
                        .setNegativeButton("Annuler",null)
                        .setPositiveButton("Oui!") { dialog, which ->
                            listeevent.removeAt(position)
                            cArrayAdapter.notifyDataSetChanged()
                            if (listeevent.size == 0) {
                                pe = null
                                bout_sync.visibility=Button.GONE
                                textReconnu=null
                            }
                            eventdialog_dialog.cancel()
                        }
                        .show()

            }
            eventdialog.setNegativeButton("Annuler",null)
            eventdialog.setPositiveButton("Ok") {dialog, which ->
                if (c_debut.time.compareTo(c_fin.time) == 0) {
                    Toast.makeText(applicationContext, "Vos modifications n'ont pas été appliquées : la durée est nulle! ", Toast.LENGTH_LONG).show();
                }
                else {
                    listeevent.get(position).debutEvent = c_debut
                    listeevent.get(position).finEvent = c_fin
                    listeevent.get(position).assignation = sp.selectedItem as String
                    val mem = listeevent.removeAt(position)
                    var i = 0
                    while (i < listeevent.size && listeevent.get(i).debutEvent.before(mem.debutEvent)) i++
                    if (i == listeevent.size) listeevent.add(mem)
                    else listeevent.add(i, mem)

                    cArrayAdapter.notifyDataSetChanged()
                }
            }
            eventdialog.create()
            eventdialog_dialog = eventdialog.show()
        }//fin listview itemclick listener




        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { view ->
            if (checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) doProcess()
            else {
                Snackbar.make(view,"Une autorisation d'utiliser la caméra est indispensable",Snackbar.LENGTH_LONG).setAction("Action", null).show()
                requestPermissions(arrayOf(Manifest.permission.CAMERA,Manifest.permission.READ_EXTERNAL_STORAGE),11)
            }
        }

        bout_sync.setOnClickListener { v ->
            if (checkSelfPermission(Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(v,"Une autorisation d'accéder au calendrier est indispensable", Snackbar.LENGTH_LONG).setAction("Action",null).show();
                requestPermissions(arrayOf(Manifest.permission.READ_CALENDAR,Manifest.permission.WRITE_CALENDAR), 12)
            }
            else {
                val confirmSyncDialog =
                    AlertDialog.Builder(this@MainActivity)
                confirmSyncDialog.setMessage("Shouaitez vous vraiment ajouter ces événements à votre calendrier? :\n${pe!!.getPlanning()}\n\n${listeevent.size} événements")
                confirmSyncDialog.setNegativeButton("Non", null)
                confirmSyncDialog.setPositiveButton("Oui!") { dialog, which ->
                    val uri: Uri = CalendarContract.Calendars.CONTENT_URI
                var target_calID = -1
                    if (sauv_preferences.getInt("TARGET_CALENDRIER_ID",-18)==-18) {
                        val criteres = "(${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} = ?)"
                        val selectionArgs = arrayOf("${CalendarContract.Calendars.CAL_ACCESS_OWNER}")
                        val cur:Cursor? = contentResolver.query(uri,EVENT_PROJECTION,criteres,selectionArgs,null)

                        //Runs throug the query
                        // Use the cursor to step through the returned records
                        //https://developer.android.com/guide/topics/providers/calendar-provider#query
                        var nom_cal:String? = null
                        if (cur != null) {
                            while(cur.moveToNext()) {
                                if (PlanningExtractor.cont(cur.getString(PROJECTION_DISPLAY_NAME_INDEX), listOf("Carrefour"))) {
                                    target_calID = cur.getLong(PROJECTION_ID_INDEX).toInt()
                                    nom_cal = cur.getString(PROJECTION_DISPLAY_NAME_INDEX)
                                }
                            }
                        }
                        if (target_calID!=-1); //Si ne contient pas de calendrier carrefour.../TODO
                        else {
                            sauv_preferences.edit().putInt("TARGET_CALENDRIER_ID",target_calID).apply()
                            AlertDialog.Builder(this@MainActivity)
                                .setMessage("Votre calendrier par défaut à été sauvegardé dans le calendrier :$nom_cal")
                                .show()
                        }
                    }//fin si cal id pas sauvegardé
                    else target_calID = sauv_preferences.getInt("TARGET_CALENDRIER_ID",-18);
                    for (poste in listeevent) {
                        val values = ContentValues().apply {
                            put(CalendarContract.Events.DTSTART,poste.debutEvent.timeInMillis)
                            put(CalendarContract.Events.DTEND,poste.finEvent.timeInMillis)
                            put(CalendarContract.Events.TITLE,poste.assignation)
                            put(CalendarContract.Events.CALENDAR_ID,target_calID)
                            put(CalendarContract.Events.EVENT_TIMEZONE,"Europe/Paris")
                        }
                        contentResolver.insert(CalendarContract.Events.CONTENT_URI,values)
                    }//fin for
                    bout_sync.isEnabled = false
                    dialog.dismiss()
                    AlertDialog.Builder(this@MainActivity)
                        .setMessage("Vos événements ont bien été importés!")
                        .setPositiveButton("Ok trop cool",null)
                        .show()
                }
                confirmSyncDialog.show()
            }//Fin permission calendrier
        }
    }//fin Oncreate(

    private fun doProcess() {
        val intention = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        var photo:File? = null
        try {
            photo = createImageFile()//créer le ficheir
        } catch (e:IOException) {
            e.printStackTrace()
        }
        if (photo!=null) {
            val photoUri = FileProvider.getUriForFile(this, "com.example.edtcarrefour.fileprovider",photo)
            intention.putExtra(MediaStore.EXTRA_OUTPUT,photoUri)
            startActivityForResult(intention,10)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(resultCode!= Activity.RESULT_CANCELED) {
            if(requestCode==10) {
                galleryAddPic()
                setPic()
            }

            if (requestCode==18) {//Ouvrir
                currentPhotoUri = data?.data
                currentPhotoPath = currentPhotoUri?.path
                apercuimg.setImageURI(currentPhotoUri)
            }
            var inputImage: InputImage? = null
            try {
                inputImage = InputImage.fromFilePath(applicationContext,currentPhotoUri)
            } catch (e:IOException) {
                e.printStackTrace()
            }
            val recognizer = TextRecognition.getClient()
            val result = recognizer.process(inputImage)
                .addOnSuccessListener {visionText ->
                    Toast.makeText(getApplicationContext(), "Texte détecté avec succès! ", Toast.LENGTH_SHORT).show();
                    processTextBlock(visionText.text)
                }
                .addOnFailureListener{ e ->
                    Toast.makeText(getApplicationContext(), "Oups.. La reconnaissance n'a pas aboutit", Toast.LENGTH_LONG).show();
                }
        }
    }//fin on activity result

    private fun processTextBlock(textReconnu:String) {
        this.textReconnu = textReconnu
        pe = null
        try {
            pe = PlanningExtractor(textReconnu.split("\n"))
        } catch (e:Exception) {
            AlertDialog.Builder(this)
                .setTitle("Impossible de traiter le texte")
                .setMessage("Une erreur fatale est survenue, vous pouvez modifier le texte pour le corriger...\n$e")
                .show()

        }
        if (pe!=null  && pe?.buildResult!=PlanningExtractor.ALLRIGHT) {
            val dialog = AlertDialog.Builder(this)
            dialog.setTitle("Oups... Erreur de traitement planning")
            if (pe?.buildResult == PlanningExtractor.DATE_NN_DETERMINEE) dialog.setMessage("L'application n'a pas pu déterminer de date de départ...La structure du texte est probablement eronnée, ou n'a pas été reconnue.\n${pe?.buildMessage}")
            else if (pe?.buildResult ==  PlanningExtractor.BREAK_OCCURED) dialog.setMessage("L'application a rencontré un problème. La structure du texte reconnu est éronnée\n ${pe?.buildMessage}")
            else dialog.setMessage(pe?.buildMessage)
            dialog.setPositiveButton("Ok",null)
            dialog.show()
        }
        else if (pe!=null) {
            listeevent.clear()
            for (e in pe?.semaine!!) listeevent.add(e)
            Toast.makeText(getApplicationContext(), "${listeevent.size} évenements détectés", Toast.LENGTH_LONG).show();
            cArrayAdapter.notifyDataSetChanged()
            bout_sync.visibility = Button.VISIBLE
            bout_sync.isEnabled = true
        }
    }


    @Throws(IOException::class)
    private fun createImageFile() : File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_$timeStamp _"
        var storageDir:File?
        try {
            storageDir= File("${Environment.getExternalStorageDirectory()}${File.separator}Pictures/EDT_pictures")
        } catch (e:Exception) {
            storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        }
        var image = File.createTempFile(imageFileName,".jpg",storageDir)
        currentPhotoUri = Uri.fromFile(image.absoluteFile)
        currentPhotoPath = image.absolutePath
        return image
    }
    private fun galleryAddPic() {
        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        val f = File(currentPhotoPath)
        val contentUri = Uri.fromFile(f)
        mediaScanIntent.setData(contentUri)
        this.sendBroadcast(mediaScanIntent)
    }
    private fun setPic() {
        val bmOptions = BitmapFactory.Options()
        bmOptions.inJustDecodeBounds = false
        bmOptions.inSampleSize=1//scale factor 1

        bmOptions.inPurgeable = true
        val bitmap = BitmapFactory.decodeFile(currentPhotoPath,bmOptions)
        apercuimg.setImageBitmap(bitmap)

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_clear -> {
                listeevent.clear()
                cArrayAdapter.notifyDataSetChanged()
                pe = null
                bout_sync.visibility=Button.GONE
                textReconnu=null
                true
            }
            R.id.action_showrecogniedtext -> {
                val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                val myScrollView = inflater.inflate(R.layout.scrollview_dialog, null, false)

                val et = myScrollView.findViewById<EditText>(R.id.edittxtfromscroll)
                et.setText("")
                var recontexte = false
                if (textReconnu != null) {
                    et.setText(textReconnu)
                    recontexte = true
                } else et.setText("Désolé, il n'ya actuellement pas de texte reconnu.")


                val textereconnudialog = AlertDialog.Builder(this@MainActivity)
                textereconnudialog.setView(myScrollView)
                et.isEnabled = recontexte
                if (recontexte) { //On n'ajoute la possibilité d'éditer le texte uniquement si du texte est reconnu

                    textereconnudialog.setPositiveButton("Ok") { dialog, which ->
                        processTextBlock(et.text.toString()) //-> redéfinit textReconnu
                    }
                    textereconnudialog.setNegativeButton("Annuler", null)
                }
                textereconnudialog.setTitle("Texte reconnu :").show()
                true
            }
            R.id.use_sampletexte -> {
                val exemple = " Varlet Nicolas 174 \n  30:00\n  35:11 \n  Du 2 3/11/2020 au 29/11/2020 (Déduction faite des Pauses) \n  Sem 48 de 2020 \n  L 23 Repos\n  00:00 \n  M 24 08:1 5-14:00 ROLL \n  05:30 \n M 25 08:30-1 4:00 ACCs\n  05:15 \n  J26 14:30-21:15 ROLL \n  06:25 \n  V 27 09:45-13:15 CAISS \n  14:15-20:15 CAISS\n  09:01 \n  S 28 09:15-1 3:30 CAISS \n  04:00 \n  D 29 09:45-15:00 ROLL \n  05:00";
                pe = PlanningExtractor(exemple.split("\n"))
                listeevent.clear()
                for (e in pe?.semaine!!) listeevent.add(e)
                cArrayAdapter.notifyDataSetChanged()
                true
            }
            R.id.ouvririmage -> {
                val intention = Intent(Intent.ACTION_PICK)
                intention.setType("image/*")
                startActivityForResult(intention,18)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    fun isDarkMode():Boolean {
        val nightModeFlags = applicationContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES
    }

    private class CustomArrayAdapter(context:Context, ressource:Int, viewRessourceId:Int,poste:MutableList<PosteEvent>,darkMode:Boolean) : ArrayAdapter<PosteEvent>(context,ressource,viewRessourceId,poste) {
        private var poste:MutableList<PosteEvent>? = null
        private var darkModeEnabled = darkMode
        init {
            this.poste=poste
            this.darkModeEnabled = darkMode
        }
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            super.getView(position, convertView, parent)

            val inflater =context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val ligneevent =inflater.inflate(R.layout.listelement, parent, false)
            val jour_strTv = ligneevent.findViewById<TextView>(R.id.fisrtlinejr)
            val jour_nbrTv = ligneevent.findViewById<TextView>(R.id.scndlinenbr)
            val titre_event = ligneevent.findViewById<TextView>(R.id.eventtitretextview)
            val horaires_event = ligneevent.findViewById<TextView>(R.id.heureeventtextview)

            val leposte = poste?.get(position)

            val formatjoursem = SimpleDateFormat("E", Locale.FRENCH)
            jour_strTv.setText(formatjoursem.format(leposte?.debutEvent!!.time).toUpperCase(Locale.ROOT))
            val formatnbr = SimpleDateFormat("d", Locale.FRENCH)//ex6 ou 17
            jour_nbrTv.setText(formatnbr.format(leposte?.debutEvent!!.time).toUpperCase(Locale.ROOT))
            if (position>0 && poste?.get(position)?.debutEvent?.get(Calendar.DAY_OF_MONTH) ==poste?.get(position-1)?.debutEvent?.get(Calendar.DAY_OF_MONTH)) {//Si deux event consécutifs ont la même date : change couleur police
                val c =  if (darkModeEnabled) Color.rgb(21,21,21) else Color.WHITE
                jour_strTv.setTextColor(c)
                jour_nbrTv.setTextColor(c)
            }
            titre_event.setText(leposte.assignation)
            horaires_event.setText(leposte.getPlageHoraire())
            return ligneevent
        }
    }


    val EVENT_PROJECTION = arrayOf( //Liste des criteres qui seront exploitables dans la query calendrier
            CalendarContract.Calendars._ID,  // 0
            CalendarContract.Calendars.ACCOUNT_NAME,  // 1
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,  // 2
            CalendarContract.Calendars.OWNER_ACCOUNT,  // 3
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL //, CalendarContract.Calendars.CAL_ACCESS_OWNER=700 -> Calendrier "compatibles" pas les fetes tout ça..
        )

    // The indices for the projection array above.
    private val PROJECTION_ID_INDEX = 0
    private val PROJECTION_ACCOUNT_NAME_INDEX = 1
    private val PROJECTION_DISPLAY_NAME_INDEX = 2
    private val PROJECTION_OWNER_ACCOUNT_INDEX = 3
    private val PROJECTION_OWNER_ACCOUNT_TYPE = 4
    private val PROJECTION_ACCESS_LEVEL = 5
}