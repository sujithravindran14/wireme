# Introduction
WireMe is a general uPnP/Dlna Server&Player for android devices. With WireMe, you can easily share media files on your android devices with others, and also you
can access other uPnP/Dlna compatible devices on your wlan network( home network for instance ).

# Development
Build from source
First check out the source code with [git](http://git-scm.com/) as the [Source Page](http://code.google.com/p/wireme/source/checkout) explained:

    $ git clone https://code.google.com/p/wireme/ wireme

We use [Eclipse](http://www.eclipse.org/) to develop, so make sure you have eclipse installed on your computer, and with
[ADT](http://developer.android.com/sdk/eclipse-adt.html), [Android SDK](http://developer.android.com/sdk/) installed and configured correctly.

Open Eclipse, Select File->Import->General->Existing Projects into Workspace, then navigate to the source directory you just checked out. Now you will see
WireMe on the left Package Explorer view window, feel free to develop with the source code.

You may get a _**The project cannot be built until the build path errors are resolved**_. Error in Eclipse, and
[here](http://www.scottdstrader.com/blog/ether_archives/000921.html) is the solution.
