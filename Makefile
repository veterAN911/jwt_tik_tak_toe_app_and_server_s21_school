ANDROID_HOME ?= $(HOME)/Library/Android/sdk
ADB := $(ANDROID_HOME)/platform-tools/adb
EMULATOR := $(ANDROID_HOME)/emulator/emulator

APP := $(CURDIR)/mobail/tiktak_app
SERVER := $(CURDIR)/server
APK := $(APP)/app/build/outputs/apk/debug/app-debug.apk
ACTIVITY := com.example.tiktak_app/.MainActivity
AVD ?= $(shell $(EMULATOR) -list-avds 2>/dev/null | head -n 1)

.PHONY: build server emulator phone status stop

build:
	cd $(APP) && ./gradlew assembleDebug

server:
	@if nc -z localhost 8099 2>/dev/null; then \
		echo "Сервер уже работает на localhost:8099"; \
	else \
		cd $(SERVER) && ./gradlew run; \
	fi

emulator:
	@set -e; \
	serial=$$($(ADB) devices | awk '$$2 == "device" && $$1 ~ /^emulator-/ {print $$1; exit}'); \
	if [ -z "$$serial" ]; then \
		if pgrep -f "qemu-system.*-avd" >/dev/null; then \
			echo "Эмулятор уже запускается, жду..."; \
		else \
			test -n "$(AVD)" || { echo "Не найден ни один AVD"; exit 1; }; \
			echo "Запускаю эмулятор $(AVD)"; \
			nohup $(EMULATOR) -avd $(AVD) >/dev/null 2>&1 & \
		fi; \
		while [ -z "$$serial" ]; do \
			sleep 1; \
			serial=$$($(ADB) devices | awk '$$2 == "device" && $$1 ~ /^emulator-/ {print $$1; exit}'); \
		done; \
	fi; \
	echo "Жду загрузки $$serial..."; \
	while [ "$$($(ADB) -s $$serial shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" != "1" ]; do sleep 1; done; \
	cd $(APP) && ./gradlew assembleDebug; \
	$(ADB) -s $$serial install -r $(APK); \
	$(ADB) -s $$serial shell am start -n $(ACTIVITY)

phone:
	@set -e; \
	serial=$$($(ADB) devices | awk '$$2 == "device" && $$1 !~ /^emulator-/ {print $$1; exit}'); \
	test -n "$$serial" || { echo "Телефон не найден: подключите USB и включите отладку"; exit 1; }; \
	echo "Телефон: $$serial"; \
	cd $(APP) && ./gradlew assembleDebug -PapiHost=127.0.0.1:8099; \
	$(ADB) -s $$serial reverse tcp:8099 tcp:8099; \
	$(ADB) -s $$serial install -r $(APK); \
	$(ADB) -s $$serial shell am start -n $(ACTIVITY)

status:
	@if nc -z localhost 8099 2>/dev/null; then \
		echo "Сервер: запущен (localhost:8099)"; \
	else \
		echo "Сервер: остановлен"; \
	fi
	@serial=$$($(ADB) devices | awk '$$2 == "device" && $$1 ~ /^emulator-/ {print $$1}' | paste -sd ' ' -); \
	if [ -n "$$serial" ]; then \
		echo "Эмулятор: запущен ($$serial)"; \
	else \
		echo "Эмулятор: остановлен"; \
	fi

stop:
	@echo "Останавливаю эмуляторы..."
	@$(ADB) devices | awk '$$2 == "device" && $$1 ~ /^emulator-/ {print $$1}' | while read serial; do \
		$(ADB) -s "$$serial" emu kill >/dev/null 2>&1 || true; \
	done; \
	pkill -f "qemu-system.*-avd" 2>/dev/null || true; \
	i=0; \
	while pgrep -f "qemu-system.*-avd" >/dev/null 2>&1 && [ $$i -lt 15 ]; do \
		sleep 1; \
		i=$$((i + 1)); \
	done
	@echo "Эмуляторы остановлены"
	@echo "Останавливаю сервер..."
	@pkill -f "io.ktor.server.netty.EngineMain" 2>/dev/null || true
	@pkill -f "server/gradle/wrapper/gradle-wrapper.jar run" 2>/dev/null || true
	@i=0; \
	while nc -z localhost 8099 2>/dev/null && [ $$i -lt 10 ]; do \
		sleep 1; \
		i=$$((i + 1)); \
	done
	@if nc -z localhost 8099 2>/dev/null; then \
		echo "Сервер: всё ещё запущен"; \
	else \
		echo "Сервер: остановлен"; \
	fi
