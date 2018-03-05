# Hermes

## Table of Contents ##
* [What is Hermes?](#TOC-WhatIsHermes)
* [Set Up Hermes](#TOC-Setup)
* [Register Users/Teams](#TOC-Register)
* [Administering Hermes](#TOC-Administration)
* [Future Features](#TOC-Future)
* [License](#TOC-License)

## <a name="TOC-WhatIsHermes"></a>What is Hermes? ##

Hermes is a Kotlin ktor application which bridges Github and Slack. Use it to automate common workflows involving those platforms.

Currently Hermes:
* Notifies reviewers in slack when they are requested to review a Pull Request in Github.
* Notifies Pull Request authors when their Pull Requests have been reviewed.
* Notifies Pull Request authors when their Pull Requests have been had changes requested.
* Enables Pull Request authors to ping the reviewers of a Pull Request from Github, when the pull request has been updated.
* Pings a Pull Request author when one of their commits fails to build. 

## <a name="TOC-Setup"></a>Set Up Hermes ##

Hermes is installed by first running the ktor application. Then the /webhook URL is added to all the organizations or repositories you wish to monitor with Hermes. Then Users and Teams are registered to Hermes using the API or the web interface.

@TODO - Detailed instructions

## <a name="TOC-Register"></a>Register Users/Teams ##

You can add a user by POSTing to the /users endpoint
You can add a user by filling out the form on the /registerUser.html page
You can see registered users by navigating to the /users endpoint from your browser
You can add a team by POSTing to the /teams endpoint
You can add a team by filling out the form on the /registerTeam.html page
You can see registered teams by navigating to the /teams endpoint from your browser

## <a name="TOC-Administration"></a>Administering Hermes ##

In the Config file you can set up an administration url to send administration messages to slack. These messages are sent when users and teams register and also when users are tagged in Pull Requests but have not yet registered.

## <a name="TOC-Samples"></a> Sample Workflows

@TODO

## <a name="TOC-Future"></a>Future Features ##

These are some features which are planned to be added to Hermes.
* Track the mean time to resolution for pull requests.
* Better front-end interface for registering users
* Allow the user to toggle on or off notifications
* Control the rereview messages to be for all reviewers, only those who haven't looked, or only those who have requested changes.
* Remove Teams and Users
* Tests!
* Messaging the user who configured a webhook if possible when the webhook is misconfigured.
* Subscribe to specific hermes events.
* Control your hermes subscription through slack.
* Add an avatar when a user is registered to display in slack.

## <a name="TOC-Future"></a>License ##

License
Copyright 2017 Hootsuite (developer.products@hootsuite.com)

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
