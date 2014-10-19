function asideHeight () {

    var screenWidth = window.innerWidth;
    var screenHeight = window.innerHeight;
    var sectionText = document.getElementById("sectionText");
    var sectionImage = document.getElementById("sectionId");
    if(screenWidth > screenHeight)
    {
        if(screenWidth >= 1600)
        {
            sectionImage.style.width = "60%";
            sectionText.style.width = "40%";
        }
        else
        {
            sectionImage.style.width = "50%";
            sectionText.style.width = "50%";
        }
    }
    else
    {
        sectionImage.style.width = "100%";
        sectionText.style.width = "100%";
    }
    var viewPortHeight;
    var asHeight;
    var asideEle;
    asideEle = document.getElementById("asTitle");
    viewPortHeight = document.getElementById("title").offsetHeight;
    //log("viewPortHeight: " + viewPortHeight);
    asHeight = document.getElementById("asTitle").scrollHeight;
    //log("Aside scrollHeight: " + asHeight);
    if(asHeight <= viewPortHeight) {
        asideEle.style.height = "100%";
    }
    else {
        asideEle.style.height = "auto";
    }

    if(document.getElementById("polAsideImg"))
    {
        asideEle.style.height = "auto";
    }
}
