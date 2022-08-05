package com.example.raise_developer

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.media.SoundPool
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.animation.addListener
import androidx.lifecycle.lifecycleScope
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpResponse
import com.apollographql.apollo3.network.http.HttpInterceptor
import com.apollographql.apollo3.network.http.HttpInterceptorChain
import com.example.graphqlsample.queries.GithubCommitQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.concurrent.thread
import kotlin.random.Random


class MainActivity : AppCompatActivity() {
    var personalMoney = 0  // 개인 자산
    var annualMoney = 2000 // 연봉. 10분에 한번 씩 올라가는거로 바꾸는게 나을듯
    var isMoneyThreadStop = false
    var threadArray = arrayListOf<Thread>()
    var isAnimationThreadStop = false

    lateinit var soundPool: SoundPool
    var soundId = 0

    inner class AuthorizationInterceptor(val token: String) : HttpInterceptor {
        override suspend fun intercept(
            request: HttpRequest,
            chain: HttpInterceptorChain
        ): HttpResponse {
            return chain.proceed(
                request.newBuilder().addHeader("Authorization", "Bearer $token").build()
            )
        }
    }

    override fun onStart() { // 일단 시작 할 때 쓰레드를 실행하게 해줬음 잔디 버튼 누르면 쓰레드 종료
        super.onStart()
        setTypingSound()

    }


    fun setTypingSound() { // 터치 시 소리 세팅
        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .build()
        soundId = soundPool.load(this, R.raw.typing_sound, 1)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_page)
        initEvent()
        characterMove()

//         GridLayout에 addView를 해줄 때는 꼭!! 각 아이템마다 margin을 설정하여 겹치지 않게 할 것!! 겹치면 뷰 지 스스로 삭제함
//         좀더 알아봐야함 뷰 위치 설정
//        val param = GridLayout.LayoutParams(GridLayout.spec(0,5),GridLayout.spec(0,5))
//        param.setMargins(13)
//        characterView.layoutParams = param

    }

    fun characterMove() {
        val character = findViewById<LinearLayout>(R.id.main_page_character)
        val characterNoteMark = findViewById<ImageView>(R.id.music_note)

        ObjectAnimator.ofFloat(character, "translationY", -600f).apply { // y축 이동
            duration = 700
            interpolator = LinearInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) { // 애니메이션이 종료되었을 때

                    ObjectAnimator.ofFloat(character, "translationX", 300f).apply { // x축 이동
                        duration = 700
                        interpolator = LinearInterpolator()
                        addListener(object : AnimatorListenerAdapter() {

                            override fun onAnimationEnd(animation: Animator?) { // 애니메이션이 종료되었을 때때
                                characterNoteMark.visibility = View.VISIBLE
                                ObjectAnimator.ofFloat(characterNoteMark, "translationY", 15f)
                                    .apply {
                                        duration = 800
                                        repeatCount = ValueAnimator.INFINITE
                                        repeatMode = ValueAnimator.REVERSE
                                        target = characterNoteMark
                                        start()
                                        val afterLocation = IntArray(2)
                                        character.getLocationInWindow(afterLocation)
                                        Log.d(
                                            "나중 캐릭터의 위치 조성민",
                                            "${afterLocation[0]},${afterLocation[1]}"
                                        )
                                        character.x = 0f
                                    }
                            }
                        })
                        start()
                    }
                }
            })
            start()
        }
    }

    fun addCharacter(name: String) {
        val frameLayout = findViewById<FrameLayout>(R.id.main_page_character_frame_layout)
        // 캐릭터 커스텀 뷰, 캐릭터 커스텀 뷰를 프레임 레이아웃에다가 넣을거임
        val characterView =
            layoutInflater.inflate(R.layout.main_page_character_view, frameLayout, false)
        //캐릭터 커스텀뷰 내의 뷰들
        val character = characterView.findViewById<LinearLayout>(R.id.character_linear_layout)
        val characterImage = characterView.findViewById<ImageView>(R.id.character_image)
        val characterName = characterView.findViewById<TextView>(R.id.character_name)
        val characterNoteMark = characterView.findViewById<ImageView>(R.id.character_music_note)

        val id = resources.getIdentifier(name, "mipmap", packageName)
        characterImage.setImageResource(id)

        val animationOne = ObjectAnimator.ofFloat(character, "translationY", -300f)
        animationOne.duration = 700
        animationOne.interpolator = LinearInterpolator() // 애니메이션 효과
        animationOne.start()

        val thread = Thread(AnimationThread(character))
        thread.start()
        frameLayout.addView(characterView)
    }


    fun setAnimation(character: View, option: Int){// x: 20~ 1050  y: 710 ~ 1530
        Log.d("Animation","df")
        if (option == 0) {
            var random = Random.nextInt(-500,500).toFloat()
            Log.d("option","${random}")
            val animationOne = ObjectAnimator.ofFloat(character, "translationX", random)
            animationOne.duration = 700

            animationOne.interpolator = LinearInterpolator() // 애니메이션 효과
            animationOne.addListener(object: AnimatorListenerAdapter(){
                override fun onAnimationEnd(animation: Animator?) {
                    var locationArray = IntArray(2)
                    character.getLocationInWindow(locationArray)
                    Log.d(" 캐릭터의 위치animation", "${locationArray[0]},${locationArray[1]}")
                    if (locationArray[0] < 20 ) {
                        character.x = -80f
                    }
                    else if (locationArray[0] > 1050) {
                        character.x = 990f
                    }
                }
            })
            animationOne.start()
        }
        else if(option == 1){
            Log.d("option","${option}")
            var random = Random.nextInt(-800,0).toFloat()
            Log.d("random","${random}")
            val animationOne = ObjectAnimator.ofFloat(character, "translationY", random)
            animationOne.duration = 700
            animationOne.interpolator = LinearInterpolator() // 애니메이션 효과
            animationOne.addListener(object: AnimatorListenerAdapter(){
                override fun onAnimationEnd(animation: Animator?) {
                    var locationArray = IntArray(2)
                    character.getLocationInWindow(locationArray)
                    Log.d(" 캐릭터의 위치animation", "${locationArray[0]},${locationArray[1]}")
                    if (locationArray[1] < 710 ) {
                        character.y = 100f
                        Log.d("if문1","${character.y}")
                    }
                    else if (locationArray[1] > 1530) {
                        character.y = 870f
                        Log.d("if문2","${character.y}")
                    }
                }
            })
            animationOne.start()
        }
        else if(option == 2){
            var randomX = Random.nextInt(-200,200).toFloat()
            var randomY = Random.nextInt(-800,0).toFloat()
            val animationOne = ObjectAnimator.ofFloat(character, "translationX", randomX)
            animationOne.duration = 700
            animationOne.interpolator = LinearInterpolator() // 애니메이션 효과
            animationOne.addListener(object: AnimatorListenerAdapter(){
                override fun onAnimationEnd(animation: Animator?) {
                    var locationArray = IntArray(2)
                    character.getLocationInWindow(locationArray)
                    Log.d(" 캐릭터의 위치animation", "${locationArray[0]},${locationArray[1]}")
                    if (locationArray[0] < 20 ) {
                        character.x = -80f
                    }
                    else if (locationArray[0] > 1050) {
                        character.x = 990f
                    }
                }
            })

            val animationTwo = ObjectAnimator.ofFloat(character, "translationY", randomY)
            animationTwo.duration = 700
            animationTwo.interpolator = LinearInterpolator() // 애니메이션 효과
            animationOne.addListener(object: AnimatorListenerAdapter(){
                override fun onAnimationEnd(animation: Animator?) {
                    var locationArray = IntArray(2)
                    character.getLocationInWindow(locationArray)
                    Log.d(" 캐릭터의 위치animation", "${locationArray[0]},${locationArray[1]}")
                    if (locationArray[1] < 710 ) {
                        character.y = 100f
                        Log.d("if문1","${character.y}")
                    }
                    else if (locationArray[1] > 1530) {
                        character.y = 870f
                        Log.d("if문2","${character.y}")
                    }
                }
            })

            animationOne.start()
            animationTwo.start()
        }
    }
    inner class AnimationThread(character: View): Runnable{ //쓰레드 클래스 isAnimationThreadStop가 false일 때 멈춤
        val myCharacter = character
        override fun run() {
            while(!isAnimationThreadStop){
                var option = Random.nextInt(0,3)
//                Log.d("option값","${option}")
                Thread.sleep(2000)
                runOnUiThread {
                    setAnimation(myCharacter, option)
                }

            }
        }
    }

    fun plusAnnualMoneyToPersonalMoney(){ // 개인 자산에 연봉 값을 더해주는 함수
        personalMoney += annualMoney
        findViewById<TextView>(R.id.main_page_text_view_personal_money).text = "${personalMoney}원"
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean { // 터치할 때마다 개인 자산의 TextView가 만원 씩 증가
        when(event?.action){
            MotionEvent.ACTION_DOWN -> {
                personalMoney += 10000
                findViewById<TextView>(R.id.main_page_text_view_personal_money).text = "${personalMoney}원"
                Log.d("좌표", "${event.x}, ${event.y}")
                soundPool.play(soundId, 1f, 1f, 1, 0, 1f) // 터치할 때마다 타자 소리
            }
            MotionEvent.ACTION_UP -> {

            }
        }
        return super.onTouchEvent(event)
    }

    fun initEvent(){
//        잔디버튼
        val grassBtn=findViewById<ImageButton>(R.id.grass_btn)
        grassBtn.setOnClickListener{
//            val intent= Intent(this,GrassCheckActivity::class.java)
//            startActivity(intent)
            Log.d("쓰레드 종료","isThreadStop = ")
            val apolloClient = ApolloClient.builder()
                .addHttpInterceptor(AuthorizationInterceptor("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"))
                .serverUrl("https://api.github.com/graphql")
                .build()

            lifecycleScope.launchWhenResumed {
                Log.d("이건 실행 됨?", "제발")
                val response = apolloClient.query(GithubCommitQuery("joh9911")).execute()

                Log.d("LaunchList", "Success ${response.data}")
                val view = layoutInflater.inflate(R.layout.commit_dialog,null)
                val dialog = AlertDialog.Builder(this@MainActivity)
                    .setView(view)
                    .create()
                view.findViewById<TextView>(R.id.text).text = response.data?.user?.contributionsCollection?.contributionCalendar.toString()
                dialog.show()
            }


        }
//        상점 버튼
        val shopButton = findViewById<ImageView>(R.id.shop_btn)
        shopButton.setOnClickListener {
            val shopDialog = ShopDialog(personalMoney)
            shopDialog.setDialogListener(object: ShopDialog.CustomViewClickListener{ // 인터페이스 상속받음

                override fun purchaseSuccess(price: String, menuName:String ,type: String) { // price 라는 아이템의 가격값을 전달 받음

                    personalMoney -= price.toInt() // 빼주고
                    findViewById<TextView>(R.id.main_page_text_view_personal_money).text = "${personalMoney}원" //적용
                    shopDialog.dismiss()
                    if (type == "employ"){
                        addCharacter(menuName)
                    }
                    else{}
                }
            })
            shopDialog.show(supportFragmentManager,"shopDialog") // 다이알로그 생성
        }
//         옵션 버튼
        val optionButton = findViewById<ImageButton>(R.id.main_page_button_option)
        optionButton.setOnClickListener {
            val optionDialog = OptionDialog()
            optionDialog.show(supportFragmentManager,"optionDialog") // 다이알로그 생성
        }
//        트로피 버튼
        val trophyBtn=findViewById<ImageView>(R.id.trophy_btn)
        trophyBtn.setOnClickListener{
            val intent= Intent(this,RankingActivity::class.java)
            startActivity(intent)
        }
//        인벤토리 버튼
        val inventoryButton = findViewById<ImageButton>(R.id.inventory_btn)
        inventoryButton.setOnClickListener{
            val inventoryDialog = InventoryDialog()
            inventoryDialog.show(supportFragmentManager,"inventoryDialog")
        }
//        퀴즈 버튼
        val quizButton = findViewById<ImageButton>(R.id.quiz_btn)
        quizButton.setOnClickListener{

        }

    }

//    fun grassShopDialogButtonEvent(){ // 잔디 상점과 연결하면 됨
//        val grassShopDialogButton = findViewById<Button>(R.id.grass_shop_dialog_button)
//        grassShopDialogButton.setOnClickListener {
//            val grassShopDialog = GrassShopDialog()
//
//            grassShopDialog.show(supportFragmentManager,"grassShopDialg")
//        }
//
//    }



}