package com.example.tallermovil

import android.content.Context
import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cursoradapter.widget.CursorAdapter

class ContactAdapter (context: Context?, c: Cursor?, flags: Int) : CursorAdapter(context, c, flags){
    private val CONTACT_ID_INDEX = 0
    val DISPLAY_NAME_INDEX = 1
    override fun newView(context: Context?, cursor: Cursor?, parent: ViewGroup?): View {
        return LayoutInflater.from(context)
            .inflate(R.layout.view_contact, parent, false)
    }
    override fun bindView(view: View?, context: Context?, cursor: Cursor?) {
        val tvIdContacto = view?.findViewById<TextView>(R.id.idContact)
        val tvNombre = view?.findViewById<TextView>(R.id.name)
        val idnum = cursor?.getInt(CONTACT_ID_INDEX)
        val nombre = cursor?.getString(DISPLAY_NAME_INDEX)
        tvIdContacto?.text = idnum?.toString()
        tvNombre?.text = nombre

    }
}