package com.example.chaqmoq.adapter

import android.content.Context
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
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
import com.example.chaqmoq.ui.customViews.WaveformView
import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.absoluteValue

class MessageListAdapter(private val userData: SharedPreferences, private val context: Context) :
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
        fun bind(message: Message, userData: SharedPreferences) {
            val hostId = userData.getString("nickname", null)
            val localZoneId = ZoneId.systemDefault()
            Log.d("check m type", message.message_type.toString())
            if (message.message_type !== null) {
                binding.messageBlock.visibility = View.GONE
                binding.voiceMessageBlock.visibility = View.VISIBLE
                binding.voiceSendTime.text = message.send_time
                binding.playBtn.setOnClickListener {
                    exoPlayer?.release()

                    exoPlayer = ExoPlayer.Builder(context).build().also { player ->
                        Log.d("hi", message.message)
                        val mediaItem = MediaItem.fromUri(message.message)
                        player.setMediaItem(mediaItem)
                        player.prepare()
                        player.play()

//                        val waveformView: WaveformView = binding.waveformView
//                        val amplitudes = readPcmAmplitudes(message.message)
//                        waveformView.setAmplitudes(amplitudes)

                        player.addListener(object : Player.Listener {
                            override fun onPlaybackStateChanged(state: Int) {
                                if (state == Player.STATE_READY) {
                                    Log.d("ExoPlayer", "Playing")
                                } else if (state == Player.STATE_ENDED) {
                                    Log.d("ExoPlayer", "Playback finished")
                                    player.release()
                                }
                            }
                        })
                    }
                }

            }
            else {
                if (message.send_time != null) {
                    val utcInstant = Instant.parse(message.send_time)
                    val localZonedDateTime = utcInstant.atZone(localZoneId)
                    val localDateTime = localZonedDateTime.toLocalDateTime()
                    val dt = LocalDateTime.parse(localDateTime.toString(), DateTimeFormatter.ISO_DATE_TIME)
                    val year = dt.year
                    val month = dt.month
                    val day = dt.monthValue

                    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
                    val timeFormatter2 = DateTimeFormatter.ofPattern("MM-dd")
                    val time = dt.format(timeFormatter)
                    val dayAndMonth = dt.format(timeFormatter2)

                    binding.sendTime.text = time

                    binding.messageText.text = message.message

                    val params = binding.messageBlock.layoutParams as LinearLayout.LayoutParams
                    if (message.sender_id == hostId) {
                        params.gravity = Gravity.START
                        binding.messageBlock.background = ContextCompat.getDrawable(context, R.drawable.background_send_message)
                    } else {
                        params.gravity = Gravity.END
                        binding.messageBlock.background = ContextCompat.getDrawable(context, R.drawable.background_receive_message)
                    }
                    binding.messageText.layoutParams = params

                    binding.executePendingBindings()
                }
            }


        }
        fun readPcmAmplitudes(filePath: String): List<Int> {
            val amplitudes = mutableListOf<Int>()
            val file = File(filePath)
            val inputStream = file.inputStream()
            val buffer = ByteArray(2) // Buffer for 16-bit samples

            while (inputStream.read(buffer) == 2) {
                // Convert 2 bytes to a 16-bit signed sample
                val sample = ByteBuffer.wrap(buffer)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .short.toInt() // Convert to Int to avoid overflow
                amplitudes.add(sample.absoluteValue)
            }

            inputStream.close()
            return amplitudes
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

