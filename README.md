# Teleother Discord Webhook

![Example](/docs/img/example.jpg "Example")
Capture the moment you teleother a friend and they accept it by sending a screenshot to a discord channel.
This plugin relies on the plugin [Dink](https://runelite.net/plugin-hub/show/dink) using the [external plugin feature](https://runelite.net/plugin-hub/show/dink#user-content-external-plugins).

## Settings
![Settings](/docs/img/settings.jpg "Settings")

#### Custom Message
The message that is sent to the webhook, supports the variables **%player%** and **%target%**.

#### Enable Webhook
If the plugin should send webhooks or not.

#### Include Screenshot
Should the webhook include a screenshot or not.

#### Webhook URL
Override the webhooks configured in Dink for external plugins.
Supports multiple webhooks that are separated by a semicolon (**;**).

#### Target Message
Overhead message on the player getting teleported, supports the variables **%player%** and **%target%**.

#### Player Message
Overhead message on the player casting the spell, supports the variables **%player%** and **%target%**.

## Dink settings
![Dink external plugin requests](/docs/img/dink_settings.jpg "Dink external plugin requests")

[Basic Dink instructions](https://github.com/pajlads/DinkPlugin#basic-setup)

For any requests to work, you need to make sure that **Enable External Plugin Notifications** is enabled.

For screenshots to be included, **Send Image** has to be either **When requested** or **Always**.

As for where to place the webhook, if you want all of your requests to go the same place, you can use the **Primary Webhook URLs**, alternatively if you want a specific webhook depending on the type, then you can use the **External Webhook Override** found under the **Webhook Overries** section.

To make sure that you have the right settings in dink, you can copy the code below and type `::dinkimport` in the chat.

```json
{
	"externalSendImage": "REQUESTED",
	"notifyExternal": true
}
```