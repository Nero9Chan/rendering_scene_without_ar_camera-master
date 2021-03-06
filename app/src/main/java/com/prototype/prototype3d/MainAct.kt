package com.prototype.prototype3d

import android.graphics.PixelFormat
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MotionEvent
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import kotlinx.android.synthetic.main.act_main.*
import java.util.concurrent.CompletableFuture
import kotlin.math.pow
import kotlin.math.sqrt


class MainAct : AppCompatActivity() {

    private lateinit var scene: Scene

    private lateinit var renderableModel: ModelRenderable
    private lateinit var renderableFuture: CompletableFuture<ModelRenderable>
    private lateinit var materialFuture: CompletableFuture<CustomMaterial>
    private lateinit var customMaterial: CustomMaterial

    private lateinit var colorButtonText: String
    private lateinit var metallicButtonText: String
    private lateinit var roughnessButtonText: String
    private lateinit var normalButtonText: String
    private lateinit var resetMaterialButtonText: String
    private var modelToggle: Boolean = true

    private lateinit var modelManager: ModelManager

    private lateinit var cube: Node
    private lateinit var cup: Node
    private lateinit var curtain: Node

    private var selectPressed: Boolean = false

    //^^^

    //vvvvv
    private var selectedName: String = ""

    private var originalX: Float = 0f
    private var originalY: Float = 0f
    var deltaX: Float = 0f
    var deltaY: Float = 0f
    private var pressTime: Long = 0
    private var rotated: Boolean = false
    private val rotationTrigger: Float = 30f
    private val rotationSpeed: Float = 0.005f
    private val rotationRatio: Float = 1.1f
    private var rotationLatch: Boolean = true
    private var timerLatch: Boolean = false
    //^^^ for rotation

    private var originalDistance: Float = -1f
    private var deltaDistance: Float = 0f
    private var distanceLatch: Boolean = false
    private var oldDeltaDistance: Float = 0f
    private val deltaZoomTrigger: Float = 3f //sensitive
    private val scaling: Float = 1.015f
    private var oldPointer0x: Float = -1f
    private var oldPointer0y: Float = -1f
    private var oldPointer1x: Float = -1f
    private var oldPointer1y: Float = -1f
    //^^^ for zooming


    private val zoomingNoise: Float = 0.01f //in zooming meeting 0.1 was used
    private var zoomingLatch: Boolean = true
    private var movingLactch: Boolean = true
    //^^^ for both zooming and moving

    private val movingNoise: Float = 1.0f
    private val movingSpeed: Float = 0.045f//0.035 0.003

    //^^^ for moving
    //^^^^ for OnTouchListener
    //^^^^^
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.act_main)

        scene = sceneView.scene
        //sceneView.setZOrderOnTop(true)
        /*var sceneHolder = sceneView.holder
        sceneHolder.setFormat(PixelFormat.TRANSPARENT)*/

        materialFuture = CustomMaterial.build(this) {
            baseColorSource = Uri.parse("textures/cube_diffuse.jpg")
            metallicSource = Uri.parse("textures/cube_metallic.jpg")
            roughnessSource = Uri.parse("textures/cube_roughness.jpg")
            normalSource = Uri.parse("textures/cube_normal.jpg")
        }

        modelManager = ModelManager(this, sceneView.scene, 5)

        /*cup = modelManager.createNode(
           "cup",
           "coffee_cup.sfb",
           false,
           Vector3(0f, -2f, -7f),
           Vector3(3f, 3f, 3f)
       )

       Log.d("checking", "MAIN cup: " + cup.toString())*/

        materialFuture = CustomMaterial.build(this) {
            baseColorSource = Uri.parse("textures/fabric_bump.jpg")
            metallicSource = Uri.parse("textures/fabric_dark.jpg")
            roughnessSource = Uri.parse("textures/fabric_mask.jpg")
            normalSource = Uri.parse("textures/fabric_sc.jpg")
        }

        cube = modelManager.createNode(
            "cube",
            "cube.sfb",
            false,
            Vector3(0f, -2f, -5f),
            Vector3(4f, 4f, 4f),
            Quaternion.axisAngle(Vector3(1f, -3.5f, 0f), 10f)
        )

        Log.d("checking", "MAIN cube: " + cube.toString())

        /*renderableFuture = modelManager.getRenderableFuture()

        renderableFuture.thenAcceptBoth(materialFuture) { renderableResult, materialResult ->
            customMaterial = materialResult
            renderableModel = renderableResult
            renderableModel.material = customMaterial.value
        }*/


        curtain = modelManager.createNode(
            "curtain",
            "curtain.sfb",
            true,
            Vector3(0f, -5f, -20f),
            Vector3(3f, 3f, 3f),
            Quaternion.axisAngle(Vector3(0f, 40f, 0f), 40f)
        )

        /*curtain = modelManager.createNode(
            "curtain",
            "curtain.sfb",
            true,
            Vector3(0f, 0f, 0f),
            Vector3(3f, 3f, 3f),
            Quaternion.axisAngle(Vector3(0f, 0f, 0f), 0f)
        )*/

        renderableFuture = modelManager.getRenderableFuture()

        renderableFuture.thenAcceptBoth(materialFuture) { renderableResult, materialResult ->
            customMaterial = materialResult
            renderableModel = renderableResult
            renderableModel.material = customMaterial.value
        }


        // Log.d("checking", "QUATERNION " + Quaternion.axisAngle(Vector3(0f, 20f, 0f), 10f))

        colorButton.setOnClickListener {
            customMaterial.switchBaseColor()
            if (customMaterial.isDefaultBaseColorMap)
                colorButton.setText(R.string.set_color)
            else
                colorButton.setText(R.string.reset_color)
        }

        metallicButton.setOnClickListener {
            customMaterial.switchMetallic()
            if (customMaterial.isDefaultMetallicMap)
                metallicButton.setText(R.string.set_metallic)
            else
                metallicButton.setText(R.string.reset_metallic)
        }

        roughnessButton.setOnClickListener {
            customMaterial.switchRoughness()
            if (customMaterial.isDefaultRoughnessMap)
                roughnessButton.setText(R.string.set_roughness)
            else
                roughnessButton.setText(R.string.reset_roughness)
        }

        normalButton.setOnClickListener {
            customMaterial.switchNormal()
            if (customMaterial.isDefaultNormalMap)
                normalButton.setText(R.string.set_normal)
            else
                normalButton.setText(R.string.reset_normal)
        }

        //resetMaterialButton.setText("Selecting: " + modelManager.getNode(modelManager.getSelectedIndex()).name)
        resetMaterialButton.setText("Press me to select model (default cube)")
        Log.d(
            "checking",
            "Selecting" + modelManager.getNode(modelManager.getSelectedIndex()).toString()
        )
        resetMaterialButton.setOnClickListener {
            /*customMaterial.reset()
            colorButton.setText(R.string.set_color)
            metallicButton.setText(R.string.set_metallic)
            roughnessButton.setText(R.string.set_roughness)
            normalButton.setText(R.string.set_normal)*/

            modelManager.selectNext()
            resetMaterialButton.setText("Selecting: " + modelManager.getNode(modelManager.getSelectedIndex()).name)
            Log.d("checking", modelManager.getNode(modelManager.getSelectedIndex()).toString())
        }

        //Log.d("checking", "CUBE" + cube.toString())
        //Log.d("checking", "CUP" + cup.toString())

        changeModelButton.setText("Change Background")
        changeModelButton.setOnClickListener {
            //Log.d("checking", "BUTTON" + cube.toString())
            //Log.d("checking", "CUBE" + cube.toString())
            //Log.d("checking", "CUP" + cup.toString())


            Log.d("checking", "pressed")

            if (modelToggle) {
                sceneView.background = resources.getDrawable(
                    resources.getIdentifier(
                        "house2", "drawable",
                        packageName
                    )
                )
                sceneView.setBackgroundColor(getColor(R.color.transparent))

            } else {
                /*sceneView.background = resources.getDrawable(
                    resources.getIdentifier(
                        "colorPrimaryDark", "color",
                        packageName
                    )
                )*/
                sceneView.setBackgroundResource(0)
                sceneView.setBackgroundColor(getColor(R.color.transparent))
                curtain.setParent(scene)
            }

            modelToggle = !modelToggle


        }

        scene.setOnTouchListener { hitTestResult, motionEvent ->
            val currentNode: Node = modelManager.getNode(modelManager.getSelectedIndex())
            Log.d(
                "checking",
                "LISTENER" + modelManager.getNode(modelManager.getSelectedIndex()).toString()
            )
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (selectedName == "")
                        selectedName = currentNode.name
                    pressTime = motionEvent.eventTime
                    Log.d("checking", pressTime.toString())
                    originalX = motionEvent.x
                    originalY = motionEvent.y
                }
                MotionEvent.ACTION_UP -> {
                    rotationLatch = true
                    zoomingLatch = true
                    movingLactch = true  //^^^ unlock/reset all latch
                    timerLatch = false //reset timerLatch
                    selectedName = ""
                }
                MotionEvent.ACTION_MOVE -> {
                    //Log.d("checking", motionEvent.pointerCount.toString())
                    if (SystemClock.uptimeMillis() - pressTime > 100) {
                        timerLatch =
                            true //if there is ONLY 1 finger after 100ms (0.1s), then rotation (1 finger) is enabled
                    }                     //if second finger is touching the model within 100ms, it considers as 2 fingers gesture and disable rotation (1 finger gesture)
                    //basically it is a buffer time for using 2-finger gesture
                    //otherwise, 1 finger rotation is easily misleded

                    if (motionEvent.pointerCount == 1 && rotationLatch && timerLatch && selectedName == currentNode.name) {
                        deltaX = originalX - motionEvent.x
                        deltaY = originalY - motionEvent.y

                        if (deltaX > rotationTrigger) { //finger dragging to left
                            currentNode.localRotation = Quaternion.multiply(
                                currentNode.localRotation,
                                Quaternion(
                                    Vector3.down(),
                                    rotationSpeed * (deltaX.pow(rotationRatio))
                                )
                            )

                            //Log.d("checking", (rotationSpeed * (deltaX.pow(rotationRatio))).toString())
                            rotated = true;
                        } else if (deltaX < -rotationTrigger) { //finger dragging to right
                            currentNode.localRotation = Quaternion.multiply(
                                currentNode.localRotation,
                                Quaternion(
                                    Vector3.up(),
                                    rotationSpeed * ((-deltaX).pow(rotationRatio))
                                )
                            )

                            //Log.d("checking", (rotationSpeed * ((-deltaX).pow(rotationRatio))).toString())
                            rotated = true;
                        }

                        if (deltaY > rotationTrigger) { //finger dragging to up
                            currentNode.localRotation = Quaternion.multiply(
                                currentNode.localRotation,
                                Quaternion(
                                    Vector3.left(),
                                    rotationSpeed * (deltaY.pow(rotationRatio))
                                )
                            )

                            //Log.d("checking", (rotationSpeed * ((deltaY).pow(rotationRatio))).toString())
                            rotated = true;
                        } else if (deltaY < -rotationTrigger) { //finger dragging to down
                            currentNode.localRotation = Quaternion.multiply(
                                currentNode.localRotation,
                                Quaternion(
                                    Vector3.right(),
                                    rotationSpeed * ((-deltaY).pow(rotationRatio))
                                )
                            )

                            //Log.d("checking", (rotationSpeed * ((-deltaY).pow(rotationRatio))).toString())
                            rotated = true;
                        }
                    } else if (motionEvent.pointerCount == 2 && selectedName == currentNode.name) {
                        /*Log.d("checking", "Pointer index 0 X: " + motionEvent.getX(0).toString())
                        Log.d("checking", "Pointer index 0 Y: " + motionEvent.getY(0).toString())
                        Log.d("checking", "Pointer index 1 X: " + motionEvent.getX(1).toString())
                        Log.d("checking", "Pointer index 1 Y: " + motionEvent.getY(1).toString())*/

                        rotationLatch = false

                        if (!distanceLatch) {
                            originalDistance = sqrt(
                                (motionEvent.getX(0) - motionEvent.getX(1)).pow(2) +
                                        (motionEvent.getY(0) - motionEvent.getY(1)).pow(2)
                            )
                            distanceLatch = true
                        }

                        deltaDistance = sqrt(
                            (motionEvent.getX(0) - motionEvent.getX(1)).pow(2) +
                                    (motionEvent.getY(0) - motionEvent.getY(1)).pow(2)
                        ) - originalDistance

                        if (oldDeltaDistance == -1f)// 0 means oldDeltaDistance has not really been initialized yet
                            oldDeltaDistance = deltaDistance

                        if (oldPointer0x == -1f || oldPointer0y == -1f || oldPointer1x == -1f || oldPointer1y == -1f) {
                            oldPointer0x = motionEvent.getX(0)
                            oldPointer0y = motionEvent.getY(0)
                            oldPointer1x = motionEvent.getX(1)
                            oldPointer1y = motionEvent.getY(1)
                        }

                        /*Log.d("checking", "0x " + (motionEvent.getX(0) - oldPointer0x).toString())
                        Log.d("checking", "1x " + (motionEvent.getX(1) - oldPointer1x).toString())
                        Log.d("checking", "0y " + (motionEvent.getY(0) - oldPointer0y).toString())
                        Log.d("checking", "1y " + (motionEvent.getY(1) - oldPointer1y).toString())*/

                        if (zoomingLatch
                            && !((motionEvent.getX(0) - oldPointer0x) > zoomingNoise && (motionEvent.getX(
                                1
                            ) - oldPointer1x) > zoomingNoise)
                            && !((motionEvent.getY(0) - oldPointer0y) > zoomingNoise && (motionEvent.getY(
                                1
                            ) - oldPointer1y) > zoomingNoise)
                            && !((motionEvent.getX(0) - oldPointer0x) < -zoomingNoise && (motionEvent.getX(
                                1
                            ) - oldPointer1x) < -zoomingNoise)
                            && !((motionEvent.getY(0) - oldPointer0y) < -zoomingNoise && (motionEvent.getY(
                                1
                            ) - oldPointer1y) < -zoomingNoise)
                        ) {
                            movingLactch = false

                            if (deltaDistance - oldDeltaDistance < -deltaZoomTrigger) {
                                //Log.d( "checking", "new fea " + (deltaDistance - oldDeltaDistance).toString() )
                                currentNode.localScale = currentNode.localScale.scaled(1 / scaling)
                                //Log.d("checking", "new scale " + currentNode.localScale)
                            } else if (deltaDistance - oldDeltaDistance > deltaZoomTrigger) {
                                //Log.d("checking", "new fea " + (deltaDistance - oldDeltaDistance).toString())
                                currentNode.localScale = currentNode.localScale.scaled(scaling)
                                //Log.d("checking", "new scale " + currentNode.localScale)
                            }
                        } else if (movingLactch) {
                            if ((motionEvent.getX(0) - oldPointer0x) > movingNoise && (motionEvent.getX(
                                    1
                                ) - oldPointer1x) > movingNoise
                            ) { //moving right
                                currentNode.localPosition = Vector3.add(
                                    currentNode.localPosition,
                                    Vector3(movingSpeed, 0f, 0f)
                                )
                                zoomingLatch = false
                            }

                            if ((motionEvent.getX(0) - oldPointer0x) < -movingNoise && (motionEvent.getX(
                                    1
                                ) - oldPointer1x) < -movingNoise
                            ) { //moving left
                                currentNode.localPosition = Vector3.add(
                                    currentNode.localPosition,
                                    Vector3(-movingSpeed, 0f, 0f)
                                )
                                zoomingLatch = false
                            }

                            if ((motionEvent.getY(0) - oldPointer0y) > movingNoise && (motionEvent.getY(
                                    1
                                ) - oldPointer1y) > movingNoise
                            ) { //moving up
                                currentNode.localPosition = Vector3.add(
                                    currentNode.localPosition,
                                    Vector3(0f, -movingSpeed, 0f)
                                )
                                zoomingLatch = false
                            }

                            if ((motionEvent.getY(0) - oldPointer0y) < -movingNoise && (motionEvent.getY(
                                    1
                                ) - oldPointer1y) < -movingNoise
                            ) {//moving down
                                currentNode.localPosition = Vector3.add(
                                    currentNode.localPosition,
                                    Vector3(0f, movingSpeed, 0f)
                                )
                                zoomingLatch = false
                            }
                        }


                        oldPointer0x = motionEvent.getX(0)
                        oldPointer0y = motionEvent.getY(0)
                        oldPointer1x = motionEvent.getX(1)
                        oldPointer1y = motionEvent.getY(1)
                        oldDeltaDistance = deltaDistance
                    }
                }
            }
            true
        }
    }


    private fun disableButton() {
        colorButton.isEnabled = false
        metallicButton.isEnabled = false
        roughnessButton.isEnabled = false
        normalButton.isEnabled = false
        resetMaterialButton.isEnabled = false


        colorButton.isClickable = false
        metallicButton.isClickable = false
        roughnessButton.isClickable = false
        normalButton.isClickable = false
        resetMaterialButton.isClickable = false

        colorButtonText = colorButton.text.toString()
        metallicButtonText = metallicButton.text.toString()
        roughnessButtonText = roughnessButton.text.toString()
        normalButtonText = normalButton.text.toString()
        resetMaterialButtonText = resetMaterialButton.text.toString()

        colorButton.text = ""
        metallicButton.text = ""
        roughnessButton.text = ""
        normalButton.text = ""
        resetMaterialButton.text = ""
    }

    private fun enableButton() {
        colorButton.isEnabled = true
        metallicButton.isEnabled = true
        roughnessButton.isEnabled = true
        normalButton.isEnabled = true
        resetMaterialButton.isEnabled = true

        colorButton.isClickable = true
        metallicButton.isClickable = true
        roughnessButton.isClickable = true
        normalButton.isClickable = true
        resetMaterialButton.isClickable = true

        colorButton.text = colorButtonText
        metallicButton.text = metallicButtonText
        roughnessButton.text = roughnessButtonText
        normalButton.text = normalButtonText
        resetMaterialButton.text = resetMaterialButtonText
    }

    override fun onPause() {
        super.onPause()
        sceneView.pause()
    }

    override fun onResume() {
        super.onResume()
        sceneView.resume()
    }
}


