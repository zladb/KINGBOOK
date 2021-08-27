package com.example.lasbetalk

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.lasbetalk.model.ChatModel
import com.example.lasbetalk.model.ChatModel.Comment
import com.example.lasbetalk.model.Friend
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_message.*
import com.example.lasbetalk.MessageActivity as th

class MessageActivity : AppCompatActivity() {


    private val fireDatabase = FirebaseDatabase.getInstance().reference
    private var chatRoomUid : String? = null
    private var destinationUid : String? = null
    private var uid : String? = null
    private var recyclerView : RecyclerView? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message)
        val imageView = findViewById<ImageView>(R.id.messageActivity_ImageView)
        val editText = findViewById<TextView>(R.id.messageActivity_editText)

        destinationUid = intent.getStringExtra("destinationUid")
        uid = Firebase.auth.currentUser?.uid.toString()
        recyclerView = findViewById(R.id.messageActivity_recyclerview)

        imageView.setOnClickListener {
            Log.d("클릭 시 dest", "$destinationUid")
            val chatModel = ChatModel()
            chatModel.users.put(uid.toString(), true)
            chatModel.users.put(destinationUid!!, true)
            val comment = Comment(uid, editText.text.toString())
            if(chatRoomUid == null){
                imageView.isEnabled = false
                fireDatabase.child("chatrooms").push().setValue(chatModel).addOnSuccessListener {
                    checkChatRoom()
                    Log.d("chatUidNull dest", "$destinationUid")
                }
            }else{
                fireDatabase.child("chatrooms").child(chatRoomUid.toString()).child("comments").push().setValue(comment)
                messageActivity_editText.text = null
                Log.d("chatUidNotNull dest", "$destinationUid")
            }

        }
        checkChatRoom()

    }
    private fun checkChatRoom(){
        fireDatabase.child("chatrooms").orderByChild("users/$uid").equalTo(true)
                .addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onCancelled(error: DatabaseError) {
            }
            override fun onDataChange(snapshot: DataSnapshot) {
                for (item in snapshot.children){
                    println(item)
                    val chatModel = item.getValue<ChatModel>()
                    if(chatModel?.users!!.containsKey(destinationUid)){
                        chatRoomUid = item.key
                        messageActivity_ImageView.isEnabled = true
                        recyclerView?.layoutManager = LinearLayoutManager(this@MessageActivity)
                        recyclerView?.adapter = RecyclerViewAdapter()
                    }
                }
            }
        })
    }
    inner class RecyclerViewAdapter : RecyclerView.Adapter<RecyclerViewAdapter.MessageViewHolder>() {

        private val comments = ArrayList<Comment>()
        private var friend : Friend? = null
        init{
            fireDatabase.child("users").child(destinationUid.toString()).addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onCancelled(error: DatabaseError) {
                }
                override fun onDataChange(snapshot: DataSnapshot) {
                    friend = snapshot.getValue<Friend>()
                    getMessageList()
                }



            })
        }

        fun getMessageList(){
            fireDatabase.child("chatrooms").child(chatRoomUid.toString()).child("comments").addValueEventListener(object : ValueEventListener{
                override fun onCancelled(error: DatabaseError) {
                }
                override fun onDataChange(snapshot: DataSnapshot) {
                    comments.clear()
                    for(data in snapshot.children){
                        val item = data.getValue<Comment>()
                        comments.add(item!!)
                        println(comments)
                    }
                    notifyDataSetChanged()
                    recyclerView?.scrollToPosition(comments.size - 1)
                }
            })
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
            val view : View = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)

            return MessageViewHolder(view)
        }
        override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
            if(comments[position].uid.equals(uid)){ // 본인 채팅
                holder.textView_message.text = comments[position].message
                holder.textView_message.setBackgroundResource(R.drawable.rightbubble)
                holder.textView_message.textSize = 20F
                holder.layout_destination.visibility = View.INVISIBLE
                holder.layout_main.gravity = Gravity.RIGHT
            }else{ // 상대방 채팅
                Glide.with(holder.itemView.context)
                        .load(friend?.profileImageUrl)
                        .apply(RequestOptions().circleCrop())
                        .into(holder.imageView_profile)
                holder.textView_name.text = friend?.name
                holder.layout_destination.visibility = View.VISIBLE
                holder.textView_message.setBackgroundResource(R.drawable.leftbubble)
                holder.textView_message.text = comments[position].message
                holder.textView_message.textSize = 20F
                holder.layout_main.gravity = Gravity.LEFT
            }


        }

        inner class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val textView_message: TextView = view.findViewById(R.id.messageItem_textView_message)
            val textView_name: TextView = view.findViewById(R.id.messageItem_textview_name)
            val imageView_profile: ImageView = view.findViewById(R.id.messageItem_imageview_profile)
            val layout_destination: LinearLayout = view.findViewById(R.id.messageItem_layout_destination)
            val layout_main: LinearLayout = view.findViewById(R.id.messageItem_linearlayout_main)
        }

        override fun getItemCount(): Int {
            return comments.size
        }

    }
}