package com.example.myapplication

import android.graphics.Bitmap
import android.content.pm.PackageManager
import android.app.Activity
import android.graphics.Canvas
import android.content.Context
import android.graphics.RectF
import android.content.DialogInterface
import android.graphics.Paint
import android.content.Intent
import android.graphics.Rect
import android.Manifest
import android.graphics.YuvImage
import android.graphics.ImageFormat
import android.annotation.SuppressLint
import android.graphics.Matrix
import android.graphics.Color
import android.graphics.BitmapFactory

import android.widget.ImageView
import android.widget.TextView
import android.os.Build
import android.text.InputType
import android.widget.EditText
import android.os.Bundle
import android.media.MediaScannerConnection
import android.view.View
import android.widget.Toast
import android.widget.ImageButton
import android.util.Size
import android.os.Environment
import android.media.Image
import android.util.Pair
import android.widget.Button
import android.net.Uri
import android.os.Handler
import android.os.Looper

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.camera.view.PreviewView
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.camera.core.CameraSelector
import androidx.lifecycle.LifecycleOwner
import androidx.camera.core.ImageAnalysis
import androidx.annotation.RequiresApi

import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.example.myapplication.ImageClassification.Recognition

import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers

import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ImageProcessor

import java.io.FileOutputStream
import java.io.File
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

import java.nio.channels.FileChannel
import java.nio.MappedByteBuffer


import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Executor

class MainActivity : AppCompatActivity() {
    var context: Context = this@MainActivity
    var ok = true
    var cam_preview: PreviewView? = null
    var camera_switch: Button? = null
    var recognize: Button? = null
    var graphicOverlay: GraphicOverlay? = null
    var isRecognizing = false
    lateinit var encodings: Array<FloatArray>
    private val savedRegistered = HashMap<String, Recognition>() //saved Faces for recognition
    private val savedFaces = HashMap<String, Bitmap?>() //saved Faces from detected faces
    var scaled: Bitmap? = null
    var showDetected = false
    var selected_cam = CameraSelector.LENS_FACING_BACK
    var preview_the_face: ImageView? = null
    var distance = 1.0f //Threshold distance between 2 faces
    var tenserfLite: Interpreter? = null
    private val handler = Handler(Looper.getMainLooper())
    var meniu: Button? = null
    var faceDetector: FaceDetector? = null
    var switchCamera = false
    var cameraProvider: ProcessCameraProvider? = null
    var face_adder: ImageButton? = null
    var isButtonPressed = true;
    private val scaledLock = Any()


    @RequiresApi(api = Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        preview_the_face = findViewById(R.id.imageView)
        face_adder = findViewById(R.id.imageButton)
        face_adder!!.visibility = View.INVISIBLE
        preview_the_face!!.visibility = View.INVISIBLE
        recognize = findViewById(R.id.detect_recognize)
        camera_switch = findViewById(R.id.switchCam)
        meniu = findViewById(R.id.meniu)
        graphicOverlay = findViewById(R.id.graphic_overlay)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != 0) {
            ActivityCompat.requestPermissions(this, arrayOf("android.permission.CAMERA"), 100)
        }
        LoadModel()



        meniu?.setOnClickListener { view: View? ->
            val actionDialogBuilder = AlertDialog.Builder(context)
            actionDialogBuilder.setTitle("Choose an Action:")

            val actions = arrayOf("Save on disk", "Select photo from gallery")
            actionDialogBuilder.setItems(actions) { dialog: DialogInterface?, selectedAction: Int ->
                performSelectedAction(
                    selectedAction
                )
            }
            actionDialogBuilder.setPositiveButton("Confirm") { dialog: DialogInterface?, which: Int ->
                showToast("Confirmed")
            }
            actionDialogBuilder.setNegativeButton("Cancel") { dialog: DialogInterface?, which: Int ->
                showToast("Canceled")
            }
            val actionDialog = actionDialogBuilder.create()
            actionDialog.show()
        }

        face_adder?.setOnClickListener { v: View? -> face_adder() }
        recognize?.setOnClickListener { v: View? ->
            if (recognize?.getText().toString() == "Recognize") {
                isRecognizing = true
                ok = true
                showDetected = false
                recognize?.text = "Detect Face"
                face_adder?.visibility = View.INVISIBLE
                preview_the_face?.visibility = View.INVISIBLE
            } else {
                isRecognizing = false
                showDetected = true
                recognize?.text = "Recognize"
                face_adder?.visibility = View.VISIBLE
                preview_the_face?.visibility = View.VISIBLE
            }
        }

        camera_switch?.setOnClickListener { v: View? ->
            if (selected_cam == CameraSelector.LENS_FACING_BACK) {
                switchCamera = true
                selected_cam = CameraSelector.LENS_FACING_FRONT
            } else {
                switchCamera = false
                selected_cam = CameraSelector.LENS_FACING_BACK
            }
            cameraProvider?.unbindAll()
            initializeCamera()
        }

        initializeCamera()
    }



    private fun performSelectedAction(selectedAction: Int) {
        if (selectedAction == 0) {
            saveFacesToGallery();
        } else if (selectedAction == 1) {
            loadFacesFromGallery()
        }
    }

    private fun saveFacesToGallery() {
        if (!savedFaces.isEmpty()) {
            for ((name, bitmap) in savedFaces) {
                saveBitmapToFile(name, bitmap)
            }
        }
    }

    private fun face_adder() {
        ok = false

        GlobalScope.launch(Dispatchers.Main) {
            val name = showNameInputDialog()

            if (name.isNotEmpty()) {
                val recognition = Recognition()
                recognition.encodings = encodings
                savedFaces[name] = scaled
                savedRegistered[name] = recognition
            }

            ok = true
        }
    }

    private suspend fun showNameInputDialog(): String {
        return kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Introduceti numele")

            //input
            val input = EditText(context)
            input.inputType = InputType.TYPE_CLASS_TEXT
            builder.setView(input)

            //buttons
            builder.setPositiveButton("ADD") { dialog, which ->
                continuation.resumeWith(Result.success(input.text.toString()))
                dialog.dismiss()
            }

            builder.setNegativeButton("Cancel") { dialog: DialogInterface, which: Int ->
                continuation.resumeWith(Result.success(""))
                dialog.dismiss()
            }

            builder.show()

            continuation.invokeOnCancellation {
                builder.setOnDismissListener(null)
            }

            builder.setOnDismissListener {
                continuation.resumeWith(Result.success(""))
            }
        }
    }

    fun LoadModel() {
            tenserfLite = loadTFLiteModel(this@MainActivity, "mobile_face_net.tflite")
            val detector_options=configureFaceDetectorOptions()
        faceDetector = createFaceDetectionClient(detector_options)
    }
    private fun loadTFLiteModel(activity: Activity, modelFileName: String): Interpreter {
        val assetManager = activity.assets

        assetManager.open(modelFileName).use { inputStream ->
            val byteBuffer = inputStream.readBytes().toByteBuffer()
            return Interpreter(byteBuffer)
        }
    }

    private fun ByteArray.toByteBuffer(): MappedByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(size)
        byteBuffer.order(ByteOrder.nativeOrder())
        byteBuffer.put(this)
        byteBuffer.flip()
        return byteBuffer as MappedByteBuffer
    }
    private fun createFaceDetectionClient(options: FaceDetectorOptions): FaceDetector {
        return FaceDetection.getClient(options)
    }
    private fun configureFaceDetectorOptions(): FaceDetectorOptions {
        return FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .build()
    }

    override fun onRequestPermissionsResult(rqCode: Int, authorization: Array<String>, results: IntArray) {
        super.onRequestPermissionsResult(rqCode, authorization, results)
        handlePermissionResult(rqCode, results)
    }

    private fun handlePermissionResult(rqCode: Int, results: IntArray) {
        if (rqCode == 100) {
            if (isPermissionGranted(results)) {
                showToast("Authorization completed for camera usage")
            } else {
                showToast("Authorization denied for camera usage")
            }
        }
    }

    private fun isPermissionGranted(results: IntArray): Boolean {
        return results.size > 0 && results[0] == 0 // permisiune acceptata
    }


    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun initializeCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cam_preview = findViewById(R.id.cam_realtime)
        cameraProviderFuture.addListener({
            try {
                val cameraProviderInstance = cameraProviderFuture.get()
                bindCameraUseCases(cameraProviderInstance, cam_preview)
            } catch (e: ExecutionException) {
                handleCameraInitializationError(e)
            } catch (e: InterruptedException) {
                handleCameraInitializationError(e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    fun bindCameraUseCases(cameraProviderInstance: ProcessCameraProvider, previewView: PreviewView?) {
        val cameraPreview = createCameraPreview()
        val lensFacingSelector = createLensFacingSelector()
        val imageAnalysis = createImageAnalysisUseCase()
        cameraPreview.setSurfaceProvider(previewView!!.surfaceProvider)
        val analysisExecutor: Executor = Executors.newSingleThreadExecutor()
        imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy: ImageProxy -> processImageAnalysis(imageProxy) }
        cameraProviderInstance.bindToLifecycle((this as LifecycleOwner), lensFacingSelector, imageAnalysis, cameraPreview)
    }

    private fun createCameraPreview(): Preview {
        return Preview.Builder().build()
    }

    private fun createLensFacingSelector(): CameraSelector {
        return CameraSelector.Builder().requireLensFacing(selected_cam).build()
    }

    private fun createImageAnalysisUseCase(): ImageAnalysis {
        return ImageAnalysis.Builder()
                .setTargetResolution(Size(640, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
    }

    private fun getInputImageFromProxy(imageProxy: ImageProxy): InputImage? {
        var inputImage: InputImage? = null
        @SuppressLint("UnsafeExperimentalUsageError") val mediaImage = imageProxy.image
        if (mediaImage != null) {
            inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        }
        return inputImage
    }

    private fun processImageAnalysis(imageProxy: ImageProxy) {
        handler.postDelayed({
            val inputImage = getInputImageFromProxy(imageProxy)
            inputImage?.let { processFaceDetection(it, imageProxy) }
        }, 0)
    }

    private fun processFaceDetection(inputImage: InputImage, imageProxy: ImageProxy) {
        faceDetector!!.process(inputImage)
            .addOnSuccessListener { faces: List<Face> -> handleFaceDetectionSuccess(faces, imageProxy) }
            .addOnFailureListener { e: Exception -> handleFaceDetectionFailure(e) }
            .addOnCompleteListener { task: Task<List<Face?>?>? ->
                synchronized(scaledLock) {
                    imageProxy.close()
                }
            }
    }

    private fun handleFaceDetectionSuccess(faces: List<Face>, imageProxy: ImageProxy) {
        if (!faces.isEmpty()) {
            val firstFace = faces[0]
            synchronized(scaledLock) {
                scaled = processDetectedFace(firstFace, imageProxy)
            }
            val boundingBox = RectF(firstFace.boundingBox)
            if (ok) {
                displayCroppedFace(scaled!!)
                recognizeImage(scaled!!, boundingBox)
            }
        } else {
            handleNoFacesDetected()
        }
    }

    private fun handleNoFacesDetected() {
        graphicOverlay!!.clearOverlay()
    }

    private fun getBitmapFromImageProxy(imageProxy: ImageProxy?): Bitmap? {
        return if (imageProxy != null) convertYUVToBitmap(imageProxy.image) else null
    }

    private fun displayCroppedFace(croppedFaceBitmap: Bitmap) {
        preview_the_face!!.setImageBitmap(croppedFaceBitmap)
    }

    private fun processDetectedFace(face: Face, imageProxy: ImageProxy): Bitmap {
        val frameBitmap = getBitmapFromImageProxy(imageProxy)
        val rotatedBitmap = transformBitmap(frameBitmap, imageProxy.imageInfo.rotationDegrees, false, false)
        val boundingBox = RectF(face.boundingBox)
        var croppedFaceBitmap = cropBitmap(rotatedBitmap, boundingBox)
        if (switchCamera) {
            croppedFaceBitmap = transformBitmap(croppedFaceBitmap, 0, switchCamera, false)
        }
        return resizeImage(croppedFaceBitmap, 112, 112)
    }

    fun resizeImage(originalBitmap: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val originalWidth = originalBitmap.width
        val originalHeight = originalBitmap.height
        val widthScale = targetWidth.toFloat() / originalWidth
        val heightScale = targetHeight.toFloat() / originalHeight
        val resizeMatrix = Matrix()
        resizeMatrix.postScale(widthScale, heightScale)
        val resizedImage = Bitmap.createBitmap(
                originalBitmap, 0, 0, originalWidth, originalHeight, resizeMatrix, false)
        originalBitmap.recycle()
        return resizedImage
    }

    private fun convertYUVToBitmap(yuvImage: Image?): Bitmap? {
        return try {
            val nv21Data = convertYUVToNV21(yuvImage)
            val yuvImageObj = YuvImage(nv21Data, ImageFormat.NV21, yuvImage!!.width, yuvImage.height, null)
            val outStream = ByteArrayOutputStream()
            yuvImageObj.compressToJpeg(Rect(0, 0, yuvImageObj.width, yuvImageObj.height), 100, outStream)
            val jpegData = outStream.toByteArray()
            BitmapFactory.decodeByteArray(jpegData, 0, jpegData.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun loadFacesFromGallery() {
        ok = false
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), 1)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == 1 && data != null) {
            val selectedImageUri = data.data
            try {
                val frameBitmap = decodeBitmapFromUri(selectedImageUri)
                processImage(frameBitmap)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun processImage(frameBitmap: Bitmap) {
        val inputImage = InputImage.fromBitmap(frameBitmap, 0)
        faceDetector!!.process(inputImage)
                .addOnSuccessListener { faces: List<Face> ->
                    if (!faces.isEmpty()) {
                        graphicOverlay!!.clearOverlay()
                        val face = faces[0]
                        val croppedFace = cropBitmap(transformBitmap(frameBitmap, 0, switchCamera, false), RectF(face.boundingBox))
                        scaled = resizeImage(croppedFace, 112, 112)
                        preview_the_face!!.setImageBitmap(scaled)
                        val boundingBox = RectF(face.boundingBox)
                        face_adder()
                        recognizeImage(scaled, boundingBox)
                    }
                }
                .addOnFailureListener { e: Exception? ->
                    ok = true
                    Toast.makeText(context, "Failed to add", Toast.LENGTH_SHORT).show()
                }
    }

    //TAKE DATA FROM LOCAL STORAGE AND CONVERT TO BITMAP
    @Throws(IOException::class)
    private fun decodeBitmapFromUri(uri: Uri?): Bitmap {
        val fileDescriptor = contentResolver.openFileDescriptor(uri!!, "r")
        val descriptor = fileDescriptor!!.fileDescriptor
        val decodedImage = BitmapFactory.decodeFileDescriptor(descriptor)
        fileDescriptor.close()
        return decodedImage
    }

    ////SAVING IMAGE TO FILE ON DEVICE
    private fun saveBitmapToFile(fileName: String, bitmap: Bitmap?) {
        val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val file = File(directory, "$fileName.png")
        try {

            val fos = FileOutputStream(file)

            bitmap!!.compress(Bitmap.CompressFormat.PNG, 100, fos)

            fos.flush()
            fos.close()

            MediaScannerConnection.scanFile(
                    this, arrayOf(file.toString()),
                    null
            ) { path: String?, uri: Uri? -> }
            Toast.makeText(this, "Saved: " + file.absolutePath, Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        savedFaces.clear()
    }

    fun recognizeImage(bitmap: Bitmap?, boundingBox: RectF?) {
        val imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(112, 112, ResizeOp.ResizeMethod.BILINEAR))
                .add(NormalizeOp(128.0f, 128.0f))
                .build()
        var inputImage = TensorImage(DataType.FLOAT32)
        inputImage.load(bitmap)
        inputImage = imageProcessor.process(inputImage)

        val imgData = inputImage.buffer

        // Run model
        val inputArray = arrayOf<Any>(imgData)
        val outputMap: MutableMap<Int, Any> = HashMap()
        encodings = Array(1) { FloatArray(192) }
        outputMap[0] = encodings
        tenserfLite!!.runForMultipleInputsOutputs(inputArray, outputMap)
        var distance_local = Float.MAX_VALUE

        //Compare new face with saved Faces.
        if (savedRegistered.size > 0) {
            val nearest = findNearest(encodings[0])
            if (nearest != null) {
                val name = nearest.first
                distance_local = nearest.second
                var confidenceVal = 0.0f
                confidenceVal = 1.0f / (1.0f + distance_local)
                val confidenceStr = java.lang.Float.toString(confidenceVal)
                if (distance_local < distance) {
                    if (isRecognizing) graphicOverlay!!.updateOverlay(boundingBox, name, confidenceStr)
                } else {
                    if (isRecognizing) graphicOverlay!!.updateOverlay(boundingBox, "Unknown", "0")
                }
            }
        }
    }

    private fun findNearest(emb: FloatArray): Pair<String, Float>? {
        var closestMatch: Pair<String, Float>? = null
        for ((name, value) in savedRegistered) {
            val knownEmb = (value.encodings as Array<FloatArray>)[0]
            val distance = calculateDistance(emb, knownEmb)
            if (closestMatch == null || distance < closestMatch.second) {
                closestMatch = Pair(name, distance)
            }
        }
        return closestMatch
    }

    private fun calculateDistance(emb1: FloatArray, emb2: FloatArray): Float {
        var distance = 0f
        for (i in emb1.indices) {
            val diff = emb1[i] - emb2[i]
            distance += diff * diff
        }
        return Math.sqrt(distance.toDouble()).toFloat()
    }

    private fun handleCameraInitializationError(e: Exception) {
        e.printStackTrace()
    }

    private fun handleFaceDetectionFailure(e: Exception) {
        e.printStackTrace()
    }

    companion object {
        private fun cropBitmap(sourceBitmap: Bitmap?, cropRect: RectF): Bitmap {
            val cropWidth = cropRect.width().toInt()
            val cropHeight = cropRect.height().toInt()
            val croppedBitmap = Bitmap.createBitmap(cropWidth, cropHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(croppedBitmap)
            val left = Math.max(0, cropRect.left.toInt())
            val top = Math.max(0, cropRect.top.toInt())
            val sourceRect = Rect(left, top, left + cropWidth, top + cropHeight)
            val destRect = Rect(0, 0, cropWidth, cropHeight)
            val paint = Paint(Paint.FILTER_BITMAP_FLAG)
            paint.color = Color.WHITE
            canvas.drawRect(destRect, paint)
            canvas.drawBitmap(sourceBitmap!!, sourceRect, destRect, paint)
            if (sourceBitmap != null && !sourceBitmap.isRecycled) {
                sourceBitmap.recycle()
            }
            return croppedBitmap
        }

        private fun transformBitmap(originalBitmap: Bitmap?, rotationDegrees: Int, flipX: Boolean, flipY: Boolean): Bitmap {
            val transformationMatrix = Matrix()
            transformationMatrix.postRotate(rotationDegrees.toFloat())
            transformationMatrix.postScale(if (flipX) -1.0f else 1.0f, if (flipY) -1.0f else 1.0f)
            val transformedBitmap = Bitmap.createBitmap(originalBitmap!!, 0, 0, originalBitmap.width, originalBitmap.height, transformationMatrix, true)
            if (transformedBitmap != originalBitmap) {
                originalBitmap.recycle()
            }
            return transformedBitmap
        }

        private fun convertYUVToNV21(yuvImage: Image?): ByteArray {
            val imageWidth = yuvImage!!.width
            val imageHeight = yuvImage.height
            var ySize = imageWidth * imageHeight
            val uvSize = imageWidth * imageHeight / 4
            val nv21Data = ByteArray(ySize + uvSize * 2)
            val yBuffer = yuvImage.planes[0].buffer // Y
            val uBuffer = yuvImage.planes[1].buffer // U
            val vBuffer = yuvImage.planes[2].buffer // V
            val rowStrideUV = yuvImage.planes[1].rowStride
            val pixelStrideUV = yuvImage.planes[1].pixelStride
            yBuffer[nv21Data, 0, ySize]
            var uvPos = ySize
            for (row in 0 until imageHeight / 2) {
                for (col in 0 until imageWidth / 2) {
                    uvPos = col * pixelStrideUV + row * rowStrideUV
                    nv21Data[ySize++] = vBuffer[uvPos]
                    nv21Data[ySize++] = uBuffer[uvPos]
                }
            }
            return nv21Data
        }
    }
}