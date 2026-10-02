package com.example.pulsecheck

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class VideoGalleryActivity : AppCompatActivity() {
    private lateinit var database: VideoDatabaseHelper
    private lateinit var recycler: RecyclerView
    private lateinit var empty: LinearLayout
    private lateinit var total: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_gallery)
        database = VideoDatabaseHelper(this)
        findViewById<View>(R.id.btn_back)?.setOnClickListener { finish() }
        recycler = findViewById(R.id.recycler_videos)
        empty = findViewById(R.id.layout_empty_state)
        total = findViewById(R.id.tv_total_videos)
        recycler.layoutManager = LinearLayoutManager(this)
        findViewById<Button>(R.id.btn_sync_cloud)?.setOnClickListener {
            android.widget.Toast.makeText(this, "Cloud sync is not configured.", android.widget.Toast.LENGTH_SHORT).show()
        }
        loadVideos()
    }

    private fun loadVideos() {
        val videos = database.getAllVideos()
        total.text = "${videos.size} video${if (videos.size == 1) "" else "s"}"
        empty.visibility = if (videos.isEmpty()) View.VISIBLE else View.GONE
        recycler.visibility = if (videos.isEmpty()) View.GONE else View.VISIBLE
        recycler.adapter = if (videos.isEmpty()) null else VideoAdapter(videos)
    }

    private inner class VideoAdapter(private val items: List<VideoRecord>) :
        RecyclerView.Adapter<VideoAdapter.Holder>() {
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): Holder =
            Holder(layoutInflater.inflate(R.layout.item_video, parent, false))
        override fun onBindViewHolder(holder: Holder, position: Int) {
            val item = items[position]
            holder.name.text = item.fileName
            holder.date.text = item.recordedAt
            holder.duration.text = item.getFormattedDuration()
            holder.size.text = item.getFormattedSize()
        }
        override fun getItemCount() = items.size
        inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.tv_file_name)
            val date: TextView = view.findViewById(R.id.tv_file_date)
            val duration: TextView = view.findViewById(R.id.tv_file_duration)
            val size: TextView = view.findViewById(R.id.tv_file_size)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::database.isInitialized) loadVideos()
    }

    override fun onDestroy() {
        if (::database.isInitialized) database.close()
        super.onDestroy()
    }
}
