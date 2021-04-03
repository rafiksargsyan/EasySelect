# EasySelect

Just swipe over the text to select it and do whatever you want :)

<img src="https://user-images.githubusercontent.com/5219842/113477854-0aac4780-9496-11eb-9241-8599ec2ae1e6.gif" height="400"/>

## Usage

```xml
.......

<com.rsargsyan.easyselect.EasySelectTextView
        android:id="@+id/demo"
        android:text="Some random text adlkfja ;adkjf a;dljf :)"
        app:selectedTextColor="@android:color/holo_green_light"
        app:selectedTextHighlightColor="@android:color/transparent"
        .......
        />

.......
```

```java

.......

EasySelectTextView demo = findViewById(R.id.demo);

// Specifies which color must be applied to the selected part of the text
demo.setSelectionTextColor(0x12345678);

// Specifieds which color must be applied to the background of selected text
demo.setSelectionTextHighlightColor(0x87654321);

// Do whatever you want with selected text
demo.setOnSelectionCompletedCallback(selectedString -> {
    Toast.makeText(MainActivity.this, selectedString, Toast.LENGTH_SHORT).show();
   
.......   
   
```

[![](https://jitpack.io/v/rafiksargsyan/EasySelect.svg)](https://jitpack.io/#rafiksargsyan/EasySelect)
