package com.example.raise_developer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpResponse
import com.apollographql.apollo3.network.http.HttpInterceptor
import com.apollographql.apollo3.network.http.HttpInterceptorChain
import com.example.graphqlsample.queries.GithubCommitQuery
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.oAuthCredential
import com.google.firebase.ktx.Firebase

class LoginActivity: AppCompatActivity() {

    val provider = OAuthProvider.newBuilder("github.com")

    lateinit var auth: FirebaseAuth

    var githubContributionData: List<GithubCommitQuery.Week>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_page)
        auth = Firebase.auth
        initEvent()
    }

    fun initEvent(){
//        로그인버튼
        val loginBtn=findViewById<TextView>(R.id.login_btn)
        loginBtn.setOnClickListener{
            auth.signOut()
            Log.d("버튼","${Firebase.auth.currentUser?.email}")
            auth.startActivityForSignInWithProvider(this, provider.build())
                .addOnSuccessListener(
                    OnSuccessListener<AuthResult?>() {
                            authResult -> auth.signInWithCredential(authResult.credential!!)
                        .addOnCompleteListener(this@LoginActivity) {task ->
                            if(task.isSuccessful) {
                                val user = Firebase.auth.currentUser?.email
                                val userId = authResult.additionalUserInfo?.username.toString() // 유저의 아이디
                                val intent= Intent(this,MainActivity::class.java)
                                intent.putExtra("userEmail",user)
                                intent.putExtra("userId",userId) // 유저 아이디 전달
                                startActivity(intent)
                                finish()
                            }
                            else {
                                Toast.makeText(this,"깃허브 로그인 실패", Toast.LENGTH_LONG).show()}
                        }
                    }
                )
                .addOnFailureListener(
                    OnFailureListener {
                        Toast.makeText(this,"Error", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }


    fun getGithubContributionInfo(id: String?){
        val token = BuildConfig.GITHUB_TOKEN
        val apolloClient = ApolloClient.builder()
            .addHttpInterceptor(AuthorizationInterceptor("${token}"))
            .serverUrl("https://api.github.com/graphql")
            .build()

        lifecycleScope.launchWhenResumed {
            val response = apolloClient.query(GithubCommitQuery("${id}")).execute()
            //바인드 서비스로 깃허브 정보 데이터 전달
            githubContributionData = response.data?.user?.contributionsCollection?.contributionCalendar?.weeks
            myService?.githubInfoMainActivityToService(githubContributionData)
        }
    }
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


    }

