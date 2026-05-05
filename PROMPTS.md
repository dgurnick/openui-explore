I want to build an android app that takes advantage of openui.com. The app should have a splash screen that shows while the backend connection is being established then opens a chatwindow.

Let's first describe the architecture in the README.md file so that I understand jwhat will be buillt and we tweak the design together before building.

The app  should take advantage of kotlin multiplatform so that we can include the results in iOS.

I want architecture diagrams in mermaid using the dark theme.

ok. let's build this in a branch called "bootstrap". Start with the backend then android. We won't build anything for iOS yet.

I want to be able to add a lot of new features so having just a docker run won't be enoiugh.

Shouldn't we add some other backend system as a bridge between the frontend and openui? If so, we would need a ktor based application. The idea is to add authentication in that BFF.