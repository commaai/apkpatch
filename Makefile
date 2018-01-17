
UNAME_S := $(shell uname -s)
ifeq ($(UNAME_S),Darwin)
	ANDROID_SDK := /Users/batman/Library/Android/sdk
else
	ANDROID_SDK=/opt/android-sdk
endif


ANDROID_LIB=$(ANDROID_SDK)/platforms/android-23"
DX=$(ANDROID_SDK)/build-tools/23.0.3/dx


SOURCEPATHS = \
	tinker/tinker-build/tinker-patch-lib/src/main/java \
	tinker/tinker-commons/src/main/java \
	tinker/third-party/aosp-dexutils/src/main/java \
	tinker/third-party/tinker-ziputils/src/main/java \
	tinker/third-party/bsdiff-util/src/main/java \
	src

JARS = \
  libs/apk-parser-lib-1.2.3.jar \
  libs/dom4j-1.6.1.jar \
  libs/dexlib2-2.1.3.jar \
  libs/guava-18.0.jar \
  libs/jsr305-1.3.9.jar \
  libs/util-2.1.3.jar

classpathify = $(subst $(eval) ,:,$(wildcard $1))


apkpatch: ApkPatch.jar
	echo "#!/bin/bash" > apkpatch
	echo 'exec java -cp ApkPatch.jar:'$(call classpathify,$(JARS)) 'ApkPatch "$$@"' >> apkpatch
	chmod +x apkpatch

ApkPatch.jar: ApkPatch.class
	rm -f ApkPatch.jar
	jar cvfm ApkPatch.jar Manifest.txt ApkPatch.class
	for srcpath in $(SOURCEPATHS); do \
		jar uvf ApkPatch.jar -C $$srcpath . ;\
	done

ApkPatch.class: ApkPatch.java
	javac \
	  -source 1.7 -target 1.7 \
		-sourcepath $(call classpathify,$(SOURCEPATHS)) \
		-classpath $(call classpathify,$(JARS)) \
	  ApkPatch.java

.PHONY: android
android: ApkPatch.android.jar

ApkPatch.android.jar: ApkPatch.jar
	rm -rf build ApkPatch.android.jar
	mkdir build
	$(DX) --dex --output=build/classes.dex ApkPatch.jar $(JARS)
	cd build && touch Manifest.txt && jar cvfm ../ApkPatch.android.jar Manifest.txt classes.dex
	rm -rf build

.PHONY: clean
clean:
	find $(SOURCEPATHS) -name '*.class' -exec rm {} \;
	rm -f ApkPatch.class ApkPatch.jar
