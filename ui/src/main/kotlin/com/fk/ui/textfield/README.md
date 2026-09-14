# TextField (`com.fk.ui.textfield`)

Form **formatting, validation, OTP, and counters** for Compose. Phase **E2**.

Conceptually aligned with iOS `FKTextField` / `FKCodeTextField` / `FKCountTextView`
(not a plain Material TextField wrapper).

## Layout

| Type | Role |
|------|------|
| `FkTextFields` | Package hub |
| `TextFieldFormat` / `TextFieldConfiguration` | Format + validation policy |
| `TextFieldFormatter` / `TextFieldValidator` | Sanitize / display + sync rules |
| `FkTextField` | Outlined field with raw/display + status messages |
| `OtpTextField` | Slot-based OTP (boxes / underlines) |
| `CountedTextArea` | Multiline + character counter |

## Usage

```kotlin
var phone by remember { mutableStateOf("") }
FkTextField(
  value = phone,
  onValueChange = { phone = it },
  onRawChange = { raw -> /* API payload */ },
  configuration = TextFieldConfiguration(
    format = TextFieldFormat.PhoneNumber,
    validationTrigger = TextFieldValidationTrigger.OnBlur,
  ),
  label = "Phone",
)

OtpTextField(value = code, onValueChange = { code = it }, onCompleted = { … })

CountedTextArea(value = bio, onValueChange = { bio = it })
```

## Notes

- Prefer **raw** values for APIs; **formatted** values for display (phone / bank / amount grouping).
- Empty input is valid; required checks belong to the form layer.
- Password format shows a Show/Hide trailing control; parent `status` overrides local validation chrome.
- OTP uses a near-invisible `BasicTextField` under slot chrome for IME / paste.
- UIKit accessories (beyond password toggle), focus linkage, and shake animation are not ported.
