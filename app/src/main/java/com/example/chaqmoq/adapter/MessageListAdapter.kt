package com.example.chaqmoq.adapter

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.chaqmoq.R
import com.example.chaqmoq.databinding.MessageItemBinding
import com.example.chaqmoq.model.Message
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.os.Handler
import android.os.Looper
import com.example.chaqmoq.model.User
import com.example.chaqmoq.utils.Firebase.downloadFileFromFirestore
import com.example.chaqmoq.utils.GlobalVariables
import com.example.news.utils.DateTimeUtils.getRelativeTime
import java.net.URLDecoder

class MessageListAdapter(private val userData: User, private val context: Context) :
    ListAdapter<Message, MessageListAdapter.MessageViewHolder>(MessageDiffCallback()) {
    private lateinit var recyclerView: RecyclerView

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = MessageItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MessageViewHolder(binding, context)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = getItem(position)
        holder.bind(message, userData)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun submitList(list: List<Message>?) {
        super.submitList(list)
        recyclerView.postDelayed({
            recyclerView.scrollToPosition(itemCount - 1)
        }, 100)
    }


    class MessageViewHolder(private val binding: MessageItemBinding, private val context: Context) :
        RecyclerView.ViewHolder(binding.root) {

        private var exoPlayer: ExoPlayer? = null
        fun bind(message: Message, user: User) {
            if (message.message_type == "audio") {
                binding.messageBlock.visibility = View.GONE
                binding.fileMessageBlock.visibility = View.GONE
                binding.voiceMessageBlock.visibility = View.VISIBLE
                if (message.send_time !== null ) binding.voiceSendTime.text = getRelativeTime(message.send_time)
                val params = binding.voiceMessageBlock.layoutParams as LinearLayout.LayoutParams
                if (message.sender_id == user.id) {
                    params.gravity = Gravity.END
                    binding.messageBlock.background = ContextCompat.getDrawable(context, R.drawable.background_send_message)
                } else {
                    params.gravity = Gravity.START
                    binding.messageBlock.background = ContextCompat.getDrawable(context, R.drawable.background_receive_message)
                }
                if (message.amplitudes !== null && message.amplitudes !== "") {
                    val amps: List<Float> = Gson().fromJson(message.amplitudes, object : TypeToken<List<Float>>() {}.type)
                    if (!amps.isEmpty()) binding.waveformView.setWaveform(amps)
                }
                binding.playBtn.setOnClickListener {
                    exoPlayer?.release()

                    exoPlayer = ExoPlayer.Builder(context).build().also { player ->
                        val mediaItem = MediaItem.fromUri(message.message)
                        player.setMediaItem(mediaItem)
                        player.prepare()
                        player.play()


                        val handleProgress = Handler(Looper.getMainLooper())
                        player.addListener(object : Player.Listener {
                            override fun onPlaybackStateChanged(state: Int) {
                                if (state == Player.STATE_READY) {
                                    binding.pauseBtn.visibility = View.VISIBLE
                                    binding.playBtn.visibility = View.GONE
                                    val runnable = object : Runnable {
                                        override fun run() {
                                            if (player.isPlaying) {
                                                val currentPosition = player.currentPosition
                                                Log.d("currentPosition", currentPosition.toFloat().toString())
                                                Log.d("duration", player.duration.toString())
                                                val progress = currentPosition.toFloat() / player.duration
                                                Log.d("progress", progress.toString())
                                                binding.waveformView.setProgress(progress)
                                                handleProgress.postDelayed(this, 50)
                                            }
                                        }
                                    }
                                    handleProgress.post(runnable)
                                } else if (state == Player.STATE_ENDED) {
                                    Log.d("ExoPlayer", "Playback finished")
                                    binding.pauseBtn.visibility = View.GONE
                                    binding.playBtn.visibility = View.VISIBLE
                                    binding.waveformView.setProgress(0.0f)
                                    player.release()
                                }
                            }
                        })
                    }
                }

            }
            else if (message.message_type == "text") {
                binding.messageBlock.visibility = View.VISIBLE
                binding.fileMessageBlock.visibility = View.GONE
                binding.voiceMessageBlock.visibility = View.GONE
                if (message.send_time != null) {
                    val time = getRelativeTime(message.send_time)

                    binding.sendTime.text = time

                    binding.messageText.text = message.message

                    val params = binding.messageBlock.layoutParams as LinearLayout.LayoutParams
                    if (message.sender_id == user.id) {
                        params.gravity = Gravity.END
                        binding.messageBlock.background = ContextCompat.getDrawable(context, R.drawable.background_send_message)
                        binding.sendTime.setTextColor(ContextCompat.getColor(context, R.color.white))
                        binding.messageText.setTextColor(ContextCompat.getColor(context, R.color.white))

                    } else {
                        params.gravity = Gravity.START
                        binding.messageBlock.background = ContextCompat.getDrawable(context, R.drawable.background_receive_message)
                    }
                    binding.messageText.layoutParams = params

                    binding.executePendingBindings()
                }
            }
            else if (message.message_type == "file") {
                binding.messageBlock.visibility = View.GONE
                binding.voiceMessageBlock.visibility = View.GONE
                binding.fileMessageBlock.visibility = View.VISIBLE
                if (message.send_time != null) {
                    val time = getRelativeTime(message.send_time)

                    binding.sendTime.text = time

                    binding.fileName.text = extractFileName(message.message)

                    val params = binding.messageBlock.layoutParams as LinearLayout.LayoutParams
                    if (message.sender_id == user.id) {
                        params.gravity = Gravity.END
                        binding.messageBlock.background = ContextCompat.getDrawable(context, R.drawable.background_send_message)
                        binding.sendTime.setTextColor(ContextCompat.getColor(context, R.color.white))
                        binding.messageText.setTextColor(ContextCompat.getColor(context, R.color.white))
                    } else {
                        params.gravity = Gravity.START
                        binding.messageBlock.background = ContextCompat.getDrawable(context, R.drawable.background_receive_message)
                    }
                    binding.messageText.layoutParams = params
                    binding.downloadBtn.setOnClickListener {
                        downloadFileFromFirestore(Uri.parse(message.message))
                    }
                    binding.executePendingBindings()
                }

            }

        }

        fun extractFileName(url: String): String? {
            val uri = Uri.parse(url)
            val encodedPath = uri.getQueryParameter("alt")

            val fullPath = uri.getQueryParameter("o") ?: uri.path?.substringAfter("/o/")?.substringBefore("?")

            fullPath?.let {
                val decodedPath = URLDecoder.decode(it, "UTF-8")
                return decodedPath.substringAfterLast('/')
            }

            return null
        }

    }

    class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }
    }

}

