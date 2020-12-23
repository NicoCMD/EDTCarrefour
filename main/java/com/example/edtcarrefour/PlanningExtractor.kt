package com.example.edtcarrefour

import java.util.Calendar
import java.util.GregorianCalendar

class PlanningExtractor(private val result :List<String>) {

    var num_sem:Int = 0
        private set
    private var c_debut: Calendar? = null
    private lateinit var targetJour:Calendar
    val semaine:MutableList<PosteEvent> = mutableListOf()
    private var indexJourSem:Int = 0
    private var headerPassed:Boolean = false
    var buildMessage:String = ""
        private set
    var buildResult:Int = ALLRIGHT
        private set


    init {
        var i = 0
        var s = 0
        var str_split:List<String>

        loop@ while(s<result.size) {
            when(eventType(result[s])){
                NUM_SEMAINE -> {
                    str_split=result[s].split(" ")
                    i=0
                    if (str_split[i].toLowerCase().equals("semaine")) {s++;continue@loop}
                    while (i<str_split.size && !str_split[i].toLowerCase().equals("sem")) i++
                    do i++ while(i<str_split.size && str_split[i].toLowerCase().equals(""))
                    if (i>=str_split.size) {buildResult=BREAK_OCCURED;"Dépassement de capacité du message : \n$+result[s]";s++; continue@loop}
                    try {
                        num_sem = Integer.valueOf(str_split[i])
                    } catch (e:NumberFormatException) {
                        num_sem=-1
                    }
                }
                DATES_DEB_FIN -> {
                    i=0
                    while(i<result[s].length && !(if (result[s].equals("")) false else Character.isDigit(result[s][i]))) i++
                    if (i == result[s].length) {buildResult=BREAK_OCCURED; buildMessage="Dépassement de capacité du message : \n"+result[s]; s++; continue@loop;}//Si on a pas trouvé de nbr on sort
                    var extractdate_debut = ""
                    var extractdate_fin = ""
                    while (i<result[s].length && extractdate_debut.length<10) { //12/34/5678+2/
                        if (result[s][i] != ' ' && (Character.isDigit(result[s][i]) || result[s][i]=='/')) extractdate_debut+=result[s][i]
                        i++
                    }
                    val debut_split = extractdate_debut.split("/")
                    if (i>= result[s].length-1) {buildResult=BREAK_OCCURED; buildMessage="Dépassement de capacité du message : \n"+result[s];s++; continue@loop;}//Si on a pas trouvé assez de nbr : c_dbut pas plein : on sort, (-1 pour le do while après)
                    c_debut = GregorianCalendar(Integer.parseInt(debut_split[2]),Integer.parseInt(debut_split[1])-1,Integer.parseInt(debut_split[0]));//y/m/d  m-1
                }
                PLANNING_SEMAINE -> {
                    i=0

                    while (i<result[s].length && result[s][i]==' ') i++
                    if ((indexJourSem==0 && result[s][i]=='L') || (indexJourSem<6 && result[s][i]== JOURSEM[indexJourSem+1])) {
                        do i++ while (i<result[s].length && result[s][i]== ' ')
                        val jour = Integer.parseInt(result[s].substring(i,i+2))
                        i+=2
                        while(targetJour.get(Calendar.DAY_OF_MONTH) != jour && indexJourSem<8) {
                            targetJour.add(Calendar.DAY_OF_MONTH,1)
                            indexJourSem++
                        }
                    }//fin Update indexJourSem
                    while (i<result[s].length && !Character.isDigit(result[s][i])) i++
                    while (result[s].length-i>12) {//12:34-67:89+3::- -> contient une affectation

                        if (result[s].length-i>8) {//"00:00 " = 6char+2 par sécurité : on décode un event (pas la duree sur le texte genre " 00:00 ")
                            //" M 24 | 08:1 5-14:00 ROLL "
                            var heureDebut = ""
                            var heureFin = ""
                            while (i<result[s].length && result[s][i] != '-' && heureDebut.length<5) {
                                if (result[s][i] != ' ') heureDebut += result[s][i]
                                i++
                            }
                            i++
                            while (i<result[s].length && heureFin.length<5) {
                                if (result[s][i] != ' ') heureFin += result[s][i]
                                i++
                            }
                            do i++ while (i<result[s].length && result[s][i] == ' ')
                            val mem = i
                            //Recherche de l'index d'éventuel fin d'assignation (exemple : "D 27 10:45-14:00 Accueil et 15:00-19:15 Accueil et")
                            while (i < result[s].length && !Character.isDigit(result[s][i])) i++
                            val affectation = result[s].substring(mem,i)

                            if (!(heureDebut[2] == ':' && heureFin[2] == ':')) {buildResult= HORAIRE_STRUCT_FAILED;buildMessage="Problème de structure horaire : $result[s]";s++; continue@loop}
                            val debut = copyCalendar(targetJour)
                            debut.set(Calendar.HOUR_OF_DAY, Integer.parseInt(heureDebut.substring(0, 2)))
                            debut.set(Calendar.MINUTE, Integer.parseInt(heureDebut.substring(3, 5)))
                            val fin = copyCalendar(targetJour)
                            fin.set(Calendar.HOUR_OF_DAY, Integer.parseInt(heureFin.substring(0, 2)))
                            fin.set(Calendar.MINUTE, Integer.parseInt(heureFin.substring(3, 5)))

                            val evenement = PosteEvent(debut,fin,affectation)
                            semaine.add(evenement)
                        }
                    }
                }//fin planning_semaine
            }//fin when
            s++
        }
        if (c_debut==null && buildResult!=BREAK_OCCURED) {
            buildMessage="Incapable de déterminer la date de départ";
            buildResult = DATE_NN_DETERMINEE
        }
    }

    companion object {//static
    fun cont(s:String, tokens:List<String>) : Boolean{
        for (element in tokens) {
            if (s.toLowerCase().contains(element.toLowerCase())) return true
        }
        return false
    }
        fun copyCalendar(c:Calendar):Calendar {
            return GregorianCalendar(c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH),c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE))
        }


        const val NUM_SEMAINE:Int = 1
        const val DATES_DEB_FIN = 2
        const val PLANNING_SEMAINE = 3

        const val DATE_NN_DETERMINEE = 4
        const val BREAK_OCCURED = 5
        const val ALLRIGHT = 6
        const val HORAIRE_STRUCT_FAILED = 7
        val JOURSEM = listOf('L','M','M','J','V','S','D')
    }

    private fun eventType(s: String):Int {
        if (headerPassed) return PLANNING_SEMAINE

        if (cont(s, listOf("sem")) && !cont(s, listOf("semaine"))) return NUM_SEMAINE

        else if (cont(s, listOf("du","au"))) return DATES_DEB_FIN

        else if (c_debut != null && cont(s, listOf("L ${if (c_debut!!.get(Calendar.DAY_OF_MONTH)<10) "0" else ""}${c_debut!!.get(Calendar.DAY_OF_MONTH)}"))) {
            headerPassed=true
            indexJourSem=0
            targetJour = copyCalendar(c_debut!!)
            return PLANNING_SEMAINE
        }
        return -1
    }
    fun getPlanning():String {
        var pla = ""
        for (e in semaine) pla+="${e.describeEvent()} \n"
        return pla
    }


}