# ForemWebView-android 💝

This is the official repository for the ForemWebView-android library which is used in [Forem Android App](https://play.google.com/store/apps/details?id=com.forem.android)


## Design ethos

ForemWebView-android is a [WebView](https://developer.android.com/guide/webapps/webview) based library designed specifically keeping in mind the [Forem Android App](https://github.com/forem/forem-android). This library can be used by an other app to use instance of a [forem](https://forem.com/) in their own android app.


## Setting up the library

### Gradle Setup

1. Add it in your root build.gradle at the end of repositories:

```
allprojects {
  repositories {
   ...
   maven { url 'https://jitpack.io' }
  }
}
```

2. Add the dependency
```
  implementation 'com.github.forem:ForemWebView-android:${latest_release}'
```

### Maven Setup
1. Adding jitpack

```
<repositories>
  <repository>
   <id>jitpack.io</id>
   <url>https://jitpack.io</url>
  </repository>
</repositories>
```

2. Add the dependency
```
<dependency>
  <groupId>com.github.forem</groupId>
  <artifactId>ForemWebView-android</artifactId>
  <version>{latest_release}</version>
</dependency>
```

**Documentation**
- [Using library in your app](https://github.com/forem/ForemWebView-android/wiki/Using-ForemWebView-android-Library)

## How to contribute

1.  Fork the project & clone locally.
2.  Create a branch, naming it either a feature or bug: `git checkout -b feature/that-new-feature` or `bug/fixing-that-bug`
3.  Code and commit your changes. Bonus points if you write a [good commit message](https://chris.beams.io/posts/git-commit/): `git commit -m 'Add some feature'`
4.  Push to the branch: `git push origin feature/that-new-feature`
5.  Create a pull request for your branch 🎉

## Contributions

We expect contributors to abide by our underlying [code of conduct](./CODE_OF_CONDUCT.md). All conversations and discussions on GitHub (issues, pull requests, etc.) must be respectful and harassment-free.

## License

This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. Please see the [LICENSE](./LICENSE) file in our repository for the full text.

Like many open source projects, we require that contributors provide us with a Contributor License Agreement (CLA). By submitting code to the DEV project, you are granting us a right to use that code under the terms of the CLA.

Our version of the CLA was adapted from the Microsoft Contributor License Agreement, which they generously made available to the public domain under Creative Commons CC0 1.0 Universal.

Any questions, please refer to our [license FAQ](https://docs.dev.to/licensing/) doc or email yo@dev.to

<br/>

<p align="center">
  <img
    alt="sloan"
    width=250px
    src="https://thepracticaldev.s3.amazonaws.com/uploads/user/profile_image/31047/af153cd6-9994-4a68-83f4-8ddf3e13f0bf.jpg"
  />
  <br/>
  <strong>Happy Coding</strong> ❤️
</p>
