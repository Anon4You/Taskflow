#include <jni.h>
#include <string>
#include <ctime>
#include <cstdlib>
#include <sstream>
#include <iomanip>

extern "C" {

JNIEXPORT jstring JNICALL
Java_com_taskflow_app_MainActivity_generateId(JNIEnv *env, jobject /* this */) {
    static int counter = 0;
    counter++;

    time_t now = time(nullptr);
    struct tm *timeinfo = localtime(&now);

    std::ostringstream oss;
    oss << "tf_"
        << (timeinfo->tm_year + 1900)
        << std::setfill('0') << std::setw(2) << (timeinfo->tm_mon + 1)
        << std::setfill('0') << std::setw(2) << timeinfo->tm_mday
        << "_"
        << std::setfill('0') << std::setw(2) << timeinfo->tm_hour
        << std::setfill('0') << std::setw(2) << timeinfo->tm_min
        << std::setfill('0') << std::setw(2) << timeinfo->tm_sec
        << "_"
        << counter;

    return env->NewStringUTF(oss.str().c_str());
}

JNIEXPORT jstring JNICALL
Java_com_taskflow_app_MainActivity_getTimestamp(JNIEnv *env, jobject /* this */) {
    time_t now = time(nullptr);
    struct tm *timeinfo = localtime(&now);

    std::ostringstream oss;
    oss << std::setfill('0') << std::setw(2) << timeinfo->tm_hour
        << ":"
        << std::setfill('0') << std::setw(2) << timeinfo->tm_min;

    return env->NewStringUTF(oss.str().c_str());
}

}
