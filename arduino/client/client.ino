#include <PololuLedStrip.h>

#define SERIES_2
#include <XBee.h>
#include <SoftwareSerial.h>

#define LED_COUNT 120
SoftwareSerial mySerial(3,2);
XBee xbee;
PololuLedStrip<4> leds;
rgb_color colors[LED_COUNT];

// create reusable response objects for responses we expect to handle 
ZBRxResponse rx = ZBRxResponse();
ModemStatusResponse msr = ModemStatusResponse();
XBeeAddress64 COORDINATOR(0, 0);

int statusLed = 12;
int errorLed = 13;

enum State {
  SIGNING_UP,
  RUNNING
} state = SIGNING_UP;

typedef void (*AnimationFunction) (void);

void nullAnimator() {}
AnimationFunction animator = nullAnimator;

const uint8_t CMD_SIGNUP = 0xAB;
const uint8_t CMD_SIGNUP_ACK = 0xAC;

const long CONNECTION_TIMEOUT = 30000;

unsigned long lastRxTime = 0;

void try_signup() {
  digitalWrite(statusLed, HIGH);
  digitalWrite(errorLed, HIGH);
  uint8_t payload[] = { 0xAB, LED_COUNT, 0x00 };
  ZBTxRequest zbTx = ZBTxRequest(COORDINATOR, payload, sizeof(payload));
  xbee.send(zbTx);
  unsigned long endTime = millis()+500;
  boolean success = false; 
  while (!success && millis() < endTime) {
    if (xbee.readPacket(endTime-millis())) {
      if (xbee.getResponse().getApiId() == ZB_RX_RESPONSE) {
        xbee.getResponse().getZBRxResponse(rx);
        if (rx.getData(0) == CMD_SIGNUP_ACK) {
          success = true;
          lastRxTime = millis();
          state = RUNNING;
          animator = nullAnimator;
        }
      }
    }
  } 
  digitalWrite(statusLed, success?HIGH:LOW);
  digitalWrite(errorLed, LOW);
}


/**
 * Histogram stuff
 */

rgb_color histogramColors[10];
uint8_t histogramSeparators[10];

void histogramAnimator() {
  uint8_t led;
  uint8_t part = 0;
  for (led = 0; led < LED_COUNT; led++) {
    while (led > histogramSeparators[part]) {
      part++;
    }
    colors[led] = histogramColors[part];
  }
  leds.write(colors, LED_COUNT);
}

void processHistogramPacket(ZBRxResponse& rx) {
  uint8_t* data = rx.getData()+1;
  uint8_t parts = *data++;
  if (parts > sizeof(histogramColors)) {
    parts = sizeof(histogramColors);
  }
  uint8_t part;
  for (part = 0; part < parts; part++) {
    histogramColors[part].red = *data++;
    histogramColors[part].green = *data++;
    histogramColors[part].blue = *data++;
  }
  for (; part < sizeof(histogramColors); part++) {
    histogramColors[part].red = 0;
    histogramColors[part].green = 0;
    histogramColors[part].blue = 0;
  }
  for (part = 0; part < parts-1; part++) {
    histogramSeparators[part] = *data++;
  }
  for (; part < sizeof(histogramSeparators); part++) {
    histogramSeparators[part] = LED_COUNT;
  }
  animator = histogramAnimator;
}



void try_receive() {
  xbee.readPacket();
  XBeeResponse& pkt = xbee.getResponse();
  if (pkt.isAvailable()) {
    if (pkt.getApiId() == ZB_RX_RESPONSE) {
      pkt.getZBRxResponse(rx);
      switch (rx.getData(0)) {
        case 0x10:
          processHistogramPacket(rx);
          break;
        default:
          Serial.print("Got unknown packet ");
          Serial.println(rx.getData(0));
      }
      lastRxTime = millis();
    }
  } else {
    if (lastRxTime + CONNECTION_TIMEOUT < millis()) {
      state = SIGNING_UP;
      digitalWrite(errorLed, HIGH);
    }
  }
}

void setup() {
  for (int i = 0; i < LED_COUNT; i++) {
    colors[i].red = 0;
    colors[i].green = 0;
    colors[i].blue = 0;
  }
  leds.write(colors, LED_COUNT);
  Serial.begin(115200);
  mySerial.begin(9600);
  xbee.setSerial(mySerial);
  pinMode(statusLed, OUTPUT);
  pinMode(errorLed, OUTPUT);
}

void loop() {
  switch (state) {
    case SIGNING_UP:
      try_signup();
      break;
    case RUNNING:
      try_receive();
      animator();
      break;
    default:
      break;
  }
}
