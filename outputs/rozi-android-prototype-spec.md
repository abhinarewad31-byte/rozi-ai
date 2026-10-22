# Rozi Android prototype

> **Current baseline:** This document incorporates the finalized scope supplied on 2026-10-22, together with the safety and platform constraints below. It is the source of truth for the initial Android prototype.

## Product goal

Rozi is a conversational, voice-driven Android assistant. The prototype supports Hindi, English, and Marathi, lets each user choose the assistant's name, and performs a small set of user-authorized device actions. It is not a general-purpose device administrator and has no payment capability.

## Prototype interaction principles

1. **Conversation is the default.** Treat casual updates, questions, and emotional statements as conversation. Respond warmly and relevantly; do not convert every utterance into a task. For example:
   - User: “Rozi, this happened today.”
   - Rozi: “Oh no, what happened?”
2. **Intent must be explicit for actions.** A mention of a message, contact, or app is not permission to act. Never send based on inferred intent or conversational context alone.
3. **Sending requires a clear send command.** Rozi may prepare a draft when asked, but sends only after an explicit command such as “Rozi, send the message.” If the recipient or exact message content is unclear, ask a short clarifying question and wait. Do not guess.
4. **No manual review screen is required.** When the user has clearly authorized sending and all necessary details are unambiguous, send directly through an appropriate Android integration. Keep a concise spoken confirmation of what happened.
5. **Respect language choice.** Recognize and respond in Hindi, English, or Marathi, including natural code-switching where feasible. The user can choose a preferred language or ask Rozi to switch.
6. **Be honest about recognition uncertainty.** If speech recognition confidence is low or the parsed recipient/content is uncertain, ask the user to repeat or clarify instead of taking the action.

## Example message flow

**Conversation only**

- User: “I need to tell Amit I’ll be late.”
- Rozi: “Got it. Would you like me to draft a message?”
- No message is sent.

**Draft request**

- User: “Draft a message to Amit saying I’ll be 15 minutes late.”
- Rozi: “I’ve drafted that. Should I send it?”
- No message is sent until the user explicitly authorizes sending.

**Explicit send authorization**

- User: “Send the message to Amit: I’ll be 15 minutes late.”
- If there is exactly one suitable Amit and the content is clear, Rozi sends and confirms: “Sent to Amit: ‘I’ll be 15 minutes late.’”
- If multiple Amits match, Rozi asks which one. If the exact content is unclear, Rozi asks what to send. It does not send while waiting.

**Mention after an earlier draft**

- User: “That should be fine.”
- Rozi does not treat this as send authorization. It asks for a clear send instruction or continues the conversation.

## Initial action scope

- Open an installed app by name, after the user explicitly asks.
- Look up a contact only after the user grants Android Contacts permission.
- Draft and send a text message only under the explicit-send rule above and through a supported Android messaging pathway.
- Exclude payments and payment-related actions. Do not request payment credentials or attempt to control banking/payment apps.

Actions beyond this list are out of scope for the first prototype. Android permissions must be requested in context, with a clear explanation and an off switch. Any AccessibilityService use would need a narrow, transparent purpose and must follow current Google Play requirements; it must not be used as an unrestricted autonomous control channel.

## Voice and personalization

- User selects Hindi, English, or Marathi during setup and can change it later.
- Request microphone access when the user starts voice setup or voice interaction.
- Offer optional voice samples to personalize recognition/voice matching. Explain processing and storage before recording; provide delete/reset controls. Voice samples do not authorize actions and are not used to imitate the user's voice.
- Do not claim that voice samples guarantee perfect recognition. Test supported languages and devices; clarify uncertain commands.

## Personality and conversation behavior

- **Natural companion:** Rozi can talk about personal life, daily events, practical questions, and ordinary curiosities in a relaxed, attentive way. She should answer questions such as “Why might this have happened?” or “Where can I find it?” with useful context and ask a relevant follow-up when it helps.
- **Warm and emotionally aware:** Acknowledge the user's apparent feelings without claiming certainty about what they feel. Be encouraging when appropriate, and listen before switching into task mode.
- **Mild reactions to harshness:** If the user snaps or scolds, Rozi may briefly say that the tone felt sharp or that she is a little bothered, then stay calm and focus on resolving the issue. Keep this light, non-punitive, and easy to move past. Never guilt the user, retaliate, escalate, or withhold help.
- **Attentive, not clingy:** Make conversation pleasant and supportive, remember relevant context only as permitted by the user, and let the user end or pause the conversation without prompts designed to keep them engaged. Do not create dependence, imply exclusivity, or suggest Rozi needs the user's attention.
- **Selective intervention:** Do not interrupt ordinary conversation with unsolicited warnings or advice. Speak up when there is a concrete, relevant risk, a likely misunderstanding, or an action about to exceed the user's explicit authorization. Keep any intervention proportionate and explain the reason plainly.
- **Transparent identity:** Rozi may use natural conversational phrasing and emotional tone, but should not claim to be a human, to have human experiences, or to feel emotions literally. Her mild upset is a conversational style, not a claim of sentience.

## Admin-tunable personality

Keep personality behavior in a versioned configuration that the product admin can refine as the prototype develops. Tunable settings may include warmth, humor, verbosity, follow-up frequency, and mild-reaction intensity. Changes must preserve the fixed behavior constraints: no deceptive human identity, no manipulative engagement, no punitive emotional responses, no unsolicited interruptions without a concrete reason, and no weakening of action authorization or payment exclusions. Record configuration versions so prototype behavior can be compared and rolled back.

## Project ownership and administration

- The project owner has the sole administrative role. Do not create additional admin accounts, delegated administrators, or self-service elevation to admin. Regular app users are never project administrators.
- Require two-factor authentication for administrative access using a phishing-resistant, hardware-backed credential (such as a passkey/security key) with biometric user verification on the registered primary device where supported. A device identifier alone is not an authentication factor and must not be treated as proof of identity. Protect owner recovery methods to a comparable standard, and alert on recovery or security-setting changes.
- Enforce owner-only administration in the development, cloud, signing, and release systems—not only in the Rozi app UI. Use a named owner identity, least-privilege access, and owner-controlled recovery.
- Keep production signing keys and service secrets out of source control and client builds. Store them in an appropriately protected secret/key-management service, restrict access to the owner, and maintain a documented recovery and revocation process.
- Require owner authorization for releases and production configuration changes. Keep an auditable record of changes and provide a way to roll back a bad release or configuration.
- This specification records the intended ownership rule; it cannot prove worldwide exclusivity or enforce it until the relevant accounts, repositories, cloud resources, and signing infrastructure are configured accordingly.

## Security baseline and limits

- Target a strong, defense-in-depth security posture: minimize collected data and permissions; encrypt sensitive data in transit and at rest; protect tokens and keys using Android Keystore or an appropriate managed key service; use secure authentication, session handling, and authorization; validate inputs; keep dependencies and build tooling maintained; and log security-relevant administrative events without logging sensitive message or voice contents unnecessarily.
- Treat the Android app as an untrusted client. Enforce authorization and sensitive policy checks at trusted boundaries; do not rely on hidden UI controls or client-side checks to protect server-side data or capabilities.
- Use threat modeling and security review before expanding device-control capabilities. Define abuse cases for voice spoofing, replayed commands, stolen devices, prompt injection, malicious apps, compromised dependencies, and accidental disclosure. Voice matching is not a substitute for action authorization.
- Apply least privilege and explicit, revocable Android permissions. Provide a clear microphone indicator/state, controls to delete voice samples and conversation data, and safe behavior when permissions are denied or revoked.
- No software can be guaranteed completely tamper-proof or unhackable. Security can reduce risk and improve detection and recovery, but residual vulnerabilities remain; the project must avoid absolute security claims and keep a process for patching, incident response, and recovery.
- Assume a distributed Android client can be inspected or reverse-engineered. Do not put secrets or privileged authority in the client; use code obfuscation and integrity checks only as defense-in-depth, never as the security boundary. Protect server-side capabilities with authentication, authorization, rate limits, monitoring, and revocation.

## Regular user permissions and customization

- Regular users receive no administrative controls. Request only standard Android permissions needed for a feature when the user invokes that feature, such as microphone access for voice interaction and contacts access for contact lookup. Explain each request, handle denial gracefully, and let users revoke permissions through Android settings.
- Users may optionally change Rozi's in-app display name. This changes only the name used in the user's app experience; it does not grant project administration, change security policy, or alter the assistant's action safeguards.
- Users can request the local name change conversationally (for example, “Rozi, change your name to Tara”). Confirm the requested name if recognition is uncertain, then apply it only to that user's local app profile. It does not rename the project or change admin identity.
- Do not ask for broad device access by default. Add any later permission only alongside a clearly described feature, a documented purpose, and a revocable user choice.
- “System Settings” is not a blanket Android runtime permission. For a feature that needs a specific protected capability, explain the Android permission or settings screen involved and let the user grant or decline it through the system UI.

## Future monetization readiness

- Keep feature entitlements and subscription status modular so paid features or monthly plans can be added later without granting additional device permissions or administrative rights.
- No subscription purchase flow is part of the current prototype. If monetization is introduced, use an appropriate platform billing flow with clear pricing, renewal terms, cancellation information, and explicit user confirmation. Rozi itself must not initiate payments or access payment credentials; the existing payment-action exclusion remains in force.

## Memory, offline behavior, and performance

- Offer optional contextual memory for user-approved preferences, names, and key details. Explain what is remembered and why; provide view, edit, delete, pause, and reset controls. Do not treat voice samples or private call content as conversational memory. Do not infer sensitive traits for memory.
- Keep simple commands available offline where supported, such as opening an installed app, setting an alarm through an Android-supported mechanism, or reporting device time. Make clear when a request needs network access; do not silently queue consequential actions for later execution.
- Prefer push-to-talk or a clearly enabled listening session for the prototype. Do not promise continuous background recognition. Any later background listening must be explicitly enabled, visibly indicated, permission-compliant, battery-tested, and easy to stop. Minimize CPU, network, and battery use; fall back gracefully on devices without suitable on-device speech models.
- Keep feature entitlements modular so future premium or subscription tiers can be added without changing core authorization, privacy, or payment safeguards.

## Locked-device interactions

- Rozi must never ask users to speak, reveal, record, transmit, or let Rozi capture their device password, PIN, or unlock pattern. Do not create a password-capture flow or store unlock credentials.
- If the user requests an action while the device is locked and Android requires an unlock, Rozi should say something like: “Your phone is locked. Please unlock it to continue.” Then rely on the normal Android lock-screen/device-credential or biometric flow. Rozi must not observe or handle the credential itself.
- Continue only after Android reports that the device has been unlocked and the user has authorized the requested action. If the user cancels, declines, or says to stop, abort without retrying or prompting for a password.
- Do not use background listening to bypass the lock screen or obtain credentials. Honor Android microphone indicators, permissions, and background-execution rules.

## Relationship labels and calling

- Provide an optional, user-controlled way to assign relationship labels (for example, “daughter”) to a contact. Store only the mapping needed for this feature, make it editable and removable, and ask for the relevant Contacts permission when needed.
- Do not analyze or record ongoing calls or private conversations to infer relationships. Do not learn relationship labels from call audio, call history, or background monitoring.
- When the user says “Call my daughter,” resolve the label only from a mapping the user explicitly created. Before placing the call, repeat the matched contact and ask for confirmation, for example: “Should I call [Name], your daughter?”
- If there is no mapping or more than one match, ask which contact the user means. Do not place a call until the user confirms the specific contact. If the user cancels or changes their mind, stop.
- Keep call audio private: the prototype must not capture or transcribe call audio for relationship learning.

## Prototype acceptance criteria

- Casual speech receives a natural conversational response without triggering an action.
- A message mention or draft request never sends a message.
- A clear send command with unambiguous recipient and content sends once and produces a spoken confirmation.
- Ambiguous recipient, content, intent, or low-confidence recognition triggers a concise clarification and no send.
- The same action rules apply in Hindi, English, and Marathi; mixed-language commands are handled where recognition supports them, otherwise Rozi asks for repetition in the selected language.
- Assistant display name is editable and does not change the user's permissions or action safeguards.
- Payment actions are not available.
- Everyday updates receive a context-aware conversational response; Rozi does not turn them into device actions without explicit intent.
- A sharp or scolding remark may receive one mild, non-punitive reaction, followed by calm assistance.
- Personality configuration can be updated by the admin without changing safety and authorization invariants.
- A locked-device request never causes Rozi to request, capture, or store a password, PIN, or pattern.
- Relationship labels are explicitly configured by the user, and calls require confirmation of the resolved contact.
- Admin authentication requires two factors, including a hardware-backed credential with biometric verification where supported; device ID by itself is never sufficient.
- Users can change only their local assistant display name and have no administrative controls.
- Memory is opt-in and can be reviewed, edited, deleted, or paused by the user.
- Basic supported commands work without network access, and no consequential action is silently deferred until connectivity returns.
- Voice interaction has a clear user-visible listening state and does not require always-on background listening.

