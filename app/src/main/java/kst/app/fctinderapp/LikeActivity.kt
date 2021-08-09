package kst.app.fctinderapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.yuyakaido.android.cardstackview.CardStackLayoutManager
import com.yuyakaido.android.cardstackview.CardStackListener
import com.yuyakaido.android.cardstackview.Direction
import kst.app.fctinderapp.databinding.ActivityLikeBinding

class LikeActivity : AppCompatActivity(), CardStackListener {

    private lateinit var binding: ActivityLikeBinding
    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var userDB: DatabaseReference

    private val adapter = CardItemAdapter()
    private val cardItems = mutableListOf<CardItem>()
    private val manager by lazy {
        CardStackLayoutManager(this,this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLikeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userDB = Firebase.database.reference.child("Users")

        initViews()
        getCurrentUserDB()
    }

    private fun initViews(){
        binding.carStackView.layoutManager = manager
        binding.carStackView.adapter = adapter

        binding.signOutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this,MainActivity::class.java))
            finish()
        }

        binding.matchListButton.setOnClickListener {
            startActivity(Intent(this,MatchedUserActivity::class.java))
        }
    }

    private fun getCurrentUserDB(){
        val currentUserDB = userDB.child(getCurrentUserID())
        currentUserDB.addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.child("name").value == null){
                    showNameInputPopup()
                    return
                }
                //유저정보 갱신
                getUnSelectedUsers()
            }
            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }

    private fun showNameInputPopup(){
        val editText = EditText(this)

        AlertDialog.Builder(this)
            .setTitle("이름을 입력해주세요")
            .setView(editText)
            .setPositiveButton("저장"){ _, _ ->
                if (editText.text.isEmpty()){
                    showNameInputPopup()
                } else {
                    saveUserName(editText.text.toString())
                }
            }
            .setCancelable(false)
            .show()
    }

    private fun saveUserName(name: String){
        val userId = getCurrentUserID()
        val currentUserDB = userDB.child(userId)
        val user = mutableMapOf<String, Any>()
        user["userId"] = userId
        user["name"] = name
        currentUserDB.updateChildren(user)

        getUnSelectedUsers()
    }

    private fun getCurrentUserID(): String {
        if (auth.currentUser == null){
            Toast.makeText(this,"로그인이 되어있지 않습니다.",Toast.LENGTH_SHORT).show()
            finish()
        }

        return auth.currentUser?.uid.orEmpty()
    }

    private fun getUnSelectedUsers(){
        userDB.addChildEventListener(object: ChildEventListener{
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                if (snapshot.child("userId").value != getCurrentUserID()
                    && snapshot.child("likedBy").child("like").hasChild(getCurrentUserID()).not()
                    && snapshot.child("likedBy").child("disLike").hasChild(getCurrentUserID()).not()){
                    val userId = snapshot.child("userId").value.toString()
                    var name = "undecided"
                    if (snapshot.child("name").value != null){
                        name = snapshot.child("name").value.toString()
                    }
                    cardItems.add(CardItem(userId,name))
                    adapter.submitList(cardItems)
                    adapter.notifyDataSetChanged()
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                cardItems.find { it.userId == snapshot.key }?.let{
                    it.userName = snapshot.child("name").value.toString()
                }
                adapter.submitList(cardItems)
                adapter.notifyDataSetChanged()
            }

            override fun onChildRemoved(snapshot: DataSnapshot) { }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) { }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun like(){
        val card = cardItems[manager.topPosition - 1]
        cardItems.removeFirst()

        userDB.child(card.userId)
            .child("likedBy")
            .child("like")
            .child(getCurrentUserID())
            .setValue(true)

        //매칭된 시점 확인
        saveMatchIfOtheruserLikedMe(card.userId)

        Toast.makeText(this, "${card.userName}님을 Like 하셨습니다.",Toast.LENGTH_SHORT).show()
    }

    private fun disLike(){
        val card = cardItems[manager.topPosition - 1]
        cardItems.removeFirst()

        userDB.child(card.userId)
            .child("likedBy")
            .child("dislike")
            .child(getCurrentUserID())
            .setValue(true)

        Toast.makeText(this, "${card.userName}님을 disLike 하셨습니다.",Toast.LENGTH_SHORT).show()
    }

    private fun saveMatchIfOtheruserLikedMe(otherUserId: String){
        val otherUserDB = userDB.child(getCurrentUserID()).child("likedBy").child("like").child(otherUserId)
        otherUserDB.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.value == true){
                    userDB.child(getCurrentUserID())
                        .child("likedBy")
                        .child("match")
                        .child(otherUserId)
                        .setValue(true)

                    userDB.child(otherUserId)
                        .child("likedBy")
                        .child("match")
                        .child(getCurrentUserID())
                        .setValue(true)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    override fun onCardSwiped(direction: Direction?) {
        when (direction){
            Direction.Right -> like()
            Direction.Left -> disLike()
            else -> {
            }
        }
    }

    override fun onCardDragging(direction: Direction?, ratio: Float) {}

    override fun onCardRewound() {}

    override fun onCardCanceled() {}

    override fun onCardAppeared(view: View?, position: Int) {}

    override fun onCardDisappeared(view: View?, position: Int) {}
}