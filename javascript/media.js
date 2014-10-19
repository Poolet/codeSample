(function() {
    this.initMedia = function() {
        log("@initMedia");
        this.play = get("play-icon");
        this.audio = get("audio");
        window.media = null;
        return this.audio.addEventListener('ended', window.audioEnded);
    };

    window.audioStatus = function(e) {
        log("audioStatus = " + e);
        if (e > 2) {
            return remove("playing");
        }
    };

    window.audioPosition = function(e) {
        var d;
        log("audioPosition = " + e);
        d = new Date();
        if ((window.lastClick + 500) < d.getTime()) {
            log("autoplay calling goNext()");
            return goNext();
        } else {
            return log("autoplay *not* calling goNext()");
        }
    };

    window.ending = false;

    window.moving = false;

    window.audioEnded = function(e) {
        log("audioEnded");
        if (window.ending || window.moving) {
            return false;
        }
        window.ending = true;
        if (has('autoplay')) {
            if (window.media) {
                log("checking position");
                window.media.getCurrentPosition(window.audioPosition);
            } else {
                log("autoplay calling goNext()");
                goNext();
            }
        } else {
            log("onending calling stopCurrent()");
            stopCurrent();
        }
        window.ending = false;
        log("exit onending");
        return true;
    };

    this.togglePlaying = function() {
        log("@togglePlaying");
        if (has("playing")) {
            log("@togglePlaying calling stopCurrent");
            return stopCurrent();
        } else {
            log("@togglePlaying calling playCurrent");
            return playCurrent();
        }
    };

    this.toggleAutoPlay = function() {
        if (has("autoplay")) {
            remove("autoplay");
            log("@toggleAutoPlay calling stopCurrent");
            return stopCurrent();
        } else {
            log("@toggleAutoPlay calling playCurrent");
            add("autoplay");
            return playCurrent();
        }
    };

    this.playCurrent = function() {
        var href, index, lastSource, path, src;
        log("playCurrent");
        add("playing");
        src = "assets/media/" + currentPage.media;
        if ((typeof device !== "undefined") && device.platform === "Android") {
            log("@playCurrent detected android");
            href = self.location.href;
            index = href.indexOf("slicIndex.html");
            path = href.substr(0, index);
            src = "assets/media/" + currentPage.media;
            log("opening: " + src);
            if (window.media !== null) {
                if (lastSource !== src) {
                    log("@playCurrent in if(window.media !== null");
                    window.media.stop();
                    window.media.release();
                }
            }
            window.media = new Media(path + src, window.audioEnded, log, window.audioStatus);
            log("@playCurrent calling window.media.play()");
            window.media.play();
            lastSource = src;
            add("playing");
            log("playing: " + path + src);
        } else {
            log("@playCurrent did not detect Android");
            if (audio.getAttribute("src") !== src) {
                audio.setAttribute("src", src);
                audio.load();
            }
            audio.play();
        }
        return true;
    };

    this.stopCurrent = function() {
        log("@stopCurrent");
        remove("playing");
        if ((typeof device !== "undefined") && device.platform === "Android") {
            if (window.media !== null) {
                window.media.pause();
            }
        }
        if (typeof audio !== "undefined" && audio !== null) {
            audio.pause();
        }
        return true;
    };

    this.onPlayPause = function() {
        log("@onPlayPause");
        if (has("playing")) {
            return stopCurrent();
        } else {
            return playCurrent();
        }
    };

    this.restartCurrent = function() {
        log("@restartCurrent");
        stopCurrent();
        if ((typeof device !== "undefined") && device.platform === "Android") {
            window.media.seekTo(0);
            playCurrent();
            return true;
        }
        audio.load();
        return playCurrent();
    };

}).call(this);