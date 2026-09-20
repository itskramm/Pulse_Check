package com.example.pulsecheck

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView

class ContactAdapter(
    private val contactList: List<contact>,
    private val deleteListener: OnDeleteListener?,
    private val dbHelper: ContactDatabaseHelper?
) : RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

    fun interface OnDeleteListener {
        fun onDelete(contactItem: contact, position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder =
        ContactViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_contact, parent, false))

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val current = contactList[position]
        holder.tvName.text = current.getName()
        holder.tvAffiliation.text = "Affiliation: ${current.getAffiliation()}"
        holder.tvNumber.text = "Contact: ${current.getNumber()}"
        if (current.hasAddress()) {
            holder.tvAddress.text = "📍 ${current.getPrimaryAddress()}"
            holder.tvAddress.visibility = View.VISIBLE
        } else holder.tvAddress.visibility = View.GONE

        holder.imgAvatar.apply {
            setImageResource(current.getImageResource())
            scaleType = ImageView.ScaleType.CENTER_CROP
            clipToOutline = true
            setOnClickListener { showAvatarPicker(it, current, holder) }
        }
        holder.btnCall.setOnClickListener {
            if (current.getNumber().isNotEmpty()) {
                it.context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${current.getNumber()}")))
            }
        }
        holder.btnChat.setOnClickListener {
            if (current.getNumber().isNotEmpty()) {
                it.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("sms:${current.getNumber()}")))
            }
        }
        holder.itemView.setOnClickListener {
            it.context.startActivity(Intent(it.context, edit_contact::class.java).apply {
                putExtra(edit_contact.EXTRA_CONTACT_ID, current.getId())
            })
        }
        holder.itemView.setOnLongClickListener {
            AlertDialog.Builder(it.context)
                .setTitle("Remove Contact")
                .setMessage("Remove ${current.getName()} from emergency contacts?")
                .setPositiveButton("Remove") { _, _ ->
                    val adapterPosition = holder.bindingAdapterPosition
                    if (adapterPosition != RecyclerView.NO_POSITION) deleteListener?.onDelete(current, adapterPosition)
                }
                .setNegativeButton("Cancel", null)
                .show()
            true
        }
    }

    private fun showAvatarPicker(anchor: View, current: contact, holder: ContactViewHolder) {
        val dp = anchor.resources.displayMetrics.density.toInt()
        val grid = GridLayout(anchor.context).apply {
            columnCount = 4
            setPadding(16 * dp, 16 * dp, 16 * dp, 16 * dp)
        }
        AVATAR_OPTIONS.forEach { resource ->
            val image = ImageView(anchor.context).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 72 * dp
                    height = 72 * dp
                    setMargins(8 * dp, 8 * dp, 8 * dp, 8 * dp)
                }
                setImageResource(resource)
                scaleType = ImageView.ScaleType.CENTER_CROP
                clipToOutline = true
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(0xFFE8EAF6.toInt())
                    if (resource == current.getImageResource()) setStroke(3 * dp, 0xFF0e6995.toInt())
                }
            }
            image.setOnClickListener {
                dbHelper?.updateAvatar(current.getName(), current.getNumber(), resource)
                current.setImageResource(resource)
                holder.imgAvatar.setImageResource(resource)
                (it.tag as? AlertDialog)?.dismiss()
            }
            grid.addView(image)
        }
        val scroll = android.widget.ScrollView(anchor.context).apply { addView(grid) }
        val dialog = AlertDialog.Builder(anchor.context)
            .setTitle("Choose Profile Picture")
            .setView(scroll)
            .setNegativeButton("Cancel", null)
            .create()
        dialog.setOnShowListener {
            dialog.window?.setLayout(
                (anchor.resources.displayMetrics.widthPixels * 0.92).toInt(),
                android.view.WindowManager.LayoutParams.WRAP_CONTENT
            )
        }
        for (i in 0 until grid.childCount) grid.getChildAt(i).tag = dialog
        dialog.show()
    }

    override fun getItemCount() = contactList.size

    class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tv_item_name)
        val tvAffiliation: TextView = itemView.findViewById(R.id.tv_item_affiliation)
        val tvNumber: TextView = itemView.findViewById(R.id.tv_item_number)
        val tvAddress: TextView = itemView.findViewById(R.id.tv_item_address)
        val imgAvatar: ImageView = itemView.findViewById(R.id.img_avatar)
        val btnCall: ImageView = itemView.findViewById(R.id.btn_call)
        val btnChat: ImageView = itemView.findViewById(R.id.btn_chat)
    }

    companion object {
        private val AVATAR_OPTIONS = intArrayOf(
            R.drawable.malepic, R.drawable.malepic2, R.drawable.malepic3,
            R.drawable.femalepic, R.drawable.femalepic2, R.drawable.femalepic3,
            R.drawable.person1, R.drawable.person2
        )
    }
}
