package com.riffbox.tv.music.ui

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import coil.transform.RoundedCornersTransformation
import com.riffbox.tv.R
import com.riffbox.tv.databinding.ItemMusicArtistBinding
import com.riffbox.tv.databinding.ItemMusicCategoryBinding
import com.riffbox.tv.databinding.ItemMusicPlaylistBinding
import com.riffbox.tv.databinding.ItemMusicTrackHorizontalBinding
import com.riffbox.tv.databinding.ItemMusicTrackVerticalBinding
import com.riffbox.tv.music.model.MusicArtist
import com.riffbox.tv.music.model.MusicCategory
import com.riffbox.tv.music.model.MusicPlaylist
import com.riffbox.tv.music.model.MusicTrack

class MusicCategoryAdapter(
    private var categories: List<MusicCategory>,
    private val onCategoryClick: (MusicCategory) -> Unit
) : RecyclerView.Adapter<MusicCategoryAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemMusicCategoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMusicCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val cat = categories[position]
        holder.binding.categoryIcon.text = cat.icon
        holder.binding.categoryTitle.text = cat.title

        val gd = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(cat.gradientStart.toInt(), cat.gradientEnd.toInt())
        ).apply {
            cornerRadius = 24f
            alpha = 210
        }
        holder.binding.root.background = gd

        holder.binding.root.setOnClickListener {
            onCategoryClick(cat)
        }
    }

    override fun getItemCount() = categories.size

    fun updateList(newList: List<MusicCategory>) {
        categories = newList
        notifyDataSetChanged()
    }
}

class MusicPlaylistAdapter(
    private var playlists: List<MusicPlaylist>,
    private val onPlaylistClick: (MusicPlaylist) -> Unit
) : RecyclerView.Adapter<MusicPlaylistAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemMusicPlaylistBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMusicPlaylistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val pl = playlists[position]
        holder.binding.playlistTitle.text = pl.title
        holder.binding.playlistSubtitle.text = pl.subtitle
        holder.binding.playlistBadge.text = pl.badge

        holder.binding.playlistImage.load(pl.coverUrl) {
            crossfade(true)
            placeholder(R.drawable.ic_riffbox_logo)
            error(R.drawable.ic_riffbox_logo)
            transformations(RoundedCornersTransformation(16f))
        }

        holder.binding.root.setOnClickListener {
            onPlaylistClick(pl)
        }
    }

    override fun getItemCount() = playlists.size

    fun updateList(newList: List<MusicPlaylist>) {
        playlists = newList
        notifyDataSetChanged()
    }
}

class MusicArtistAdapter(
    private var artists: List<MusicArtist>,
    private val onArtistClick: (MusicArtist) -> Unit
) : RecyclerView.Adapter<MusicArtistAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemMusicArtistBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMusicArtistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val artist = artists[position]
        holder.binding.artistName.text = artist.name
        holder.binding.artistGenre.text = artist.genre

        holder.binding.artistImage.load(artist.imageUrl) {
            crossfade(true)
            placeholder(R.drawable.ic_riffbox_logo)
            error(R.drawable.ic_riffbox_logo)
            transformations(CircleCropTransformation())
        }

        holder.binding.root.setOnClickListener {
            onArtistClick(artist)
        }
    }

    override fun getItemCount() = artists.size

    fun updateList(newList: List<MusicArtist>) {
        artists = newList
        notifyDataSetChanged()
    }
}

class MusicTrackHorizontalAdapter(
    private var tracks: List<MusicTrack>,
    private val onTrackClick: (MusicTrack) -> Unit
) : RecyclerView.Adapter<MusicTrackHorizontalAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemMusicTrackHorizontalBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMusicTrackHorizontalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val track = tracks[position]
        holder.binding.trackTitle.text = track.title
        holder.binding.trackArtist.text = track.artist

        holder.binding.trackImage.load(track.thumbnailUrl) {
            crossfade(true)
            placeholder(R.drawable.ic_riffbox_logo)
            error(R.drawable.ic_riffbox_logo)
            transformations(RoundedCornersTransformation(14f))
        }

        holder.binding.root.setOnClickListener {
            onTrackClick(track)
        }
    }

    override fun getItemCount() = tracks.size

    fun updateList(newList: List<MusicTrack>) {
        tracks = newList
        notifyDataSetChanged()
    }
}

class MusicTrackVerticalAdapter(
    private var tracks: List<MusicTrack>,
    private val isTvDevice: Boolean,
    private val onTrackClick: (MusicTrack) -> Unit,
    private val onDownloadClick: ((MusicTrack) -> Unit)? = null,
    private val onDeleteClick: ((MusicTrack) -> Unit)? = null
) : RecyclerView.Adapter<MusicTrackVerticalAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemMusicTrackVerticalBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMusicTrackVerticalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val track = tracks[position]
        holder.binding.trackTitle.text = track.title
        holder.binding.trackArtist.text = if (track.duration.isNotEmpty()) "${track.artist} • ${track.duration}" else track.artist

        holder.binding.trackThumb.load(track.thumbnailUrl) {
            crossfade(true)
            placeholder(R.drawable.ic_riffbox_logo)
            error(R.drawable.ic_riffbox_logo)
            transformations(RoundedCornersTransformation(12f))
        }

        // Hide download/delete button on Android TV; show on phones
        if (isTvDevice) {
            holder.binding.btnDownload.visibility = View.GONE
        } else {
            holder.binding.btnDownload.visibility = View.VISIBLE
            if (track.isDownloaded) {
                holder.binding.btnDownload.setImageResource(R.drawable.ic_delete)
                holder.binding.btnDownload.setOnClickListener {
                    onDeleteClick?.invoke(track)
                }
            } else {
                holder.binding.btnDownload.setImageResource(R.drawable.ic_download)
                holder.binding.btnDownload.setOnClickListener {
                    onDownloadClick?.invoke(track)
                }
            }
        }

        holder.binding.root.setOnClickListener {
            onTrackClick(track)
        }
    }

    override fun getItemCount() = tracks.size

    fun updateList(newList: List<MusicTrack>) {
        tracks = newList
        notifyDataSetChanged()
    }
}
