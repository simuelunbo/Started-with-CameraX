package com.example.cameraxapp

import android.app.Activity
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.cameraxapp.adapter.ImageViewPagerAdapter
import com.example.cameraxapp.databinding.ActivityImageListBinding
import com.example.cameraxapp.util.PhotoPathUtil
import java.io.File
import java.io.FileNotFoundException

class ImageListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageListBinding
    private lateinit var imageViewPagerAdapter: ImageViewPagerAdapter
    private val uriList by lazy<List<Uri>> { intent.getParcelableArrayListExtra(URI_LIST_KEY)!! }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageListBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    private fun initViews() {
        setSupportActionBar(binding.toolbar)
        setupImageList()
    }

    private fun setupImageList() = with(binding) {
        if (::imageViewPagerAdapter.isInitialized.not()) {
            imageViewPagerAdapter = ImageViewPagerAdapter(uriList)
        }
        imageViewPager.adapter = imageViewPagerAdapter
        indicator.setViewPager(imageViewPager)
        imageViewPager.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() { // 현재 위치에 따라서 포지션 값 변경
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                toolbar.title = getString(
                    R.string.images_page,
                    position + 1,
                    imageViewPagerAdapter.uriList.size
                )
            }
        })
        deleteButton.setOnClickListener {
            removeImage(uriList[imageViewPager.currentItem])
        }
    }

    private fun removeImage(uri: Uri) {
        try {
            val file = File(PhotoPathUtil.getPath(this, uri) ?: throw FileNotFoundException())
            file.delete() // 실질적으로 이미지 컨텐트가 사라진거지 삭제가 된것이 아니라 따로 삭제 코드 구현해야함
            imageViewPagerAdapter.uriList.let {
                val imageList = it.toMutableList()
                val position = imageList.indexOf(uri)
                imageList.remove(uri)
                imageViewPagerAdapter.uriList = imageList
                imageViewPagerAdapter.notifyItemChanged(position)
            }
            MediaScannerConnection.scanFile(this, arrayOf(file.path), arrayOf("image/jpeg"), null)
            binding.indicator.setViewPager(binding.imageViewPager)
            if (imageViewPagerAdapter.uriList.isEmpty()) {
                finish()
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            Toast.makeText(this, getString(R.string.image_not_found), Toast.LENGTH_SHORT).show()
        }
    }


    companion object {
        private const val URI_LIST_KEY = "uriList"

        fun newIntent(activity: Activity, uriList: List<Uri>) =
            Intent(activity, ImageListActivity::class.java).apply {
                putExtra(URI_LIST_KEY, ArrayList<Uri>().apply {
                    uriList.forEach {
                        add(it)
                    }
                })
            }
    }


}