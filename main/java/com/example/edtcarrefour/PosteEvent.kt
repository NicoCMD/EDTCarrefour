package com.example.edtcarrefour

import java.text.SimpleDateFormat
import java.util.Calendar

class PosteEvent(var debutEvent: Calendar, var finEvent:Calendar, poste:String) {

    var assignation:String = poste
        set(value) {
            if (PlanningExtractor.cont(value, listOf("ACC"))) field = "Accueil"
            else if (PlanningExtractor.cont(value, listOf("ROLL"))) field = "Roller";
            else if (PlanningExtractor.cont(value, listOf("CAISS"))) field = "Caisse";
            else if (PlanningExtractor.cont(value, listOf("SCA"))) field = "Scan";
            else if (PlanningExtractor.cont(value, listOf("CLS","SCO"))) field = "Cls";
            else if (PlanningExtractor.cont(value, listOf("FORM"))) field = "Formation";
        }


    init {
        if (finEvent.time.before(debutEvent.time)) println("wtf dude, get your dates straight")
        this.assignation = poste
    }

    fun describeEvent(): String{
        val formatDate = SimpleDateFormat("dd/MM/yy")
        val formatHeure = SimpleDateFormat("HH:mm")
        return "$assignation le ${formatDate.format(debutEvent.time)} de ${formatHeure.format(debutEvent.time)} à ${formatHeure.format(finEvent.time)}"
    }
    fun getPlageHoraire():String {
        val formatHeure = SimpleDateFormat("HH:mm")
        return " de ${formatHeure.format(debutEvent.time)} à ${formatHeure.format(finEvent.time)}"
    }

    companion object {
        val ROLES = listOf("Accueil","Roller","Caisse","Scan","Cls","Formation")

        fun getIndexAssign(s:String):Int {
            for (i in ROLES.indices) if (s.equals(ROLES[i])) return i
            return 0
        }
    }

}