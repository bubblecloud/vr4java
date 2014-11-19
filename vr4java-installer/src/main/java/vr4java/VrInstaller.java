package vr4java;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.*;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import org.eclipse.aether.version.Version;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by tlaukkan on 11/6/2014.
 */
public class VrInstaller {

    public static void main(final String[] args) throws Exception{
        final VrSplash vrSplash = new VrSplash();
        final LocalRepository localRepository = new LocalRepository("repo");

        final List<RemoteRepository> remoteRepositories = Arrays.asList(
                new RemoteRepository.Builder("central", "default", "http://central.maven.org/maven2/").build(),
                new RemoteRepository.Builder("EclipseLink", "default", "http://download.eclipse.org/rt/eclipselink/maven.repo").build(),
                new RemoteRepository.Builder("bubblecloud", "default", "http://repository-bubblecloud.forge.cloudbees.com/release/").build());

        final DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService( RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class );
        locator.addService( TransporterFactory.class, FileTransporterFactory.class );
        locator.addService( TransporterFactory.class, HttpTransporterFactory.class );
        locator.setErrorHandler( new DefaultServiceLocator.ErrorHandler() {
            @Override
            public void serviceCreationFailed( Class<?> type, Class<?> impl, Throwable exception ) {
                System.out.println("Error creating " + impl + " :" + exception);
            }
        } );

        final RepositorySystem system = locator.getService(RepositorySystem.class);

        final DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        session.setLocalRepositoryManager( system.newLocalRepositoryManager( session, localRepository ) );
        session.setTransferListener( new ConsoleTransferListener() );
        session.setRepositoryListener( new ConsoleRepositoryListener(vrSplash, System.out) );
        download("installer.jar","installer-new.jar", "org.bubblecloud.vr4java", "vr4java-installer", false, remoteRepositories, system, session);
        download("client.jar", "client.jar", "org.bubblecloud.vr4java", "vr4java-client", true, remoteRepositories, system, session);

        Runtime.getRuntime().exec("java -jar client.jar");
    }

    private static void download(final String oldFile, final String newFile, String groupId, String artifactId, boolean dependencies, List<RemoteRepository> remoteRepositories, RepositorySystem system, DefaultRepositorySystemSession session) throws VersionRangeResolutionException, DependencyResolutionException, IOException, ArtifactResolutionException {
        final Artifact artifactVersionRange = new DefaultArtifact(groupId + ":" + artifactId + ":[0,)");

        final VersionRangeRequest rangeRequest = new VersionRangeRequest();
        rangeRequest.setArtifact(artifactVersionRange);
        rangeRequest.setRepositories(remoteRepositories);

        session.setUpdatePolicy(RepositoryPolicy.UPDATE_POLICY_ALWAYS);
        final VersionRangeResult rangeResult = system.resolveVersionRange( session, rangeRequest );

        final Version newestVersion = rangeResult.getHighestVersion();

        final String version = newestVersion.toString();
        System.out.println( "Newest version " + version + " from repository "
                + rangeResult.getRepository( newestVersion ));

        final String artifactFullId = groupId + ":" + artifactId + ":" + version;
        final Artifact artifact = new DefaultArtifact(artifactFullId);

        final DependencyFilter classpathFilter = DependencyFilterUtils.classpathFilter(JavaScopes.COMPILE);

        final List<ArtifactResult> artifactResults;
        if (dependencies) {
            CollectRequest collectRequest = new CollectRequest();
            collectRequest.setRoot( new Dependency( artifact, JavaScopes.COMPILE ) );
            collectRequest.setRepositories(remoteRepositories);
            final DependencyRequest dependencyRequest = new DependencyRequest(collectRequest, classpathFilter);
            session.setUpdatePolicy(RepositoryPolicy.UPDATE_POLICY_DAILY);
            artifactResults = system.resolveDependencies(session, dependencyRequest).getArtifactResults();
        } else {
            final ArtifactRequest artifactRequest = new ArtifactRequest(artifact, remoteRepositories, null);
            artifactResults = Collections.singletonList(system.resolveArtifact(session, artifactRequest));
        }
        File mainArtifactFile = null;
        for ( ArtifactResult artifactResult : artifactResults )
        {
            final File repositoryFile = artifactResult.getArtifact().getFile();
            final File destinationOldFile;
            final File destinationNewFile;
            if (groupId.equals(artifactResult.getArtifact().getGroupId()) &&
                    artifactId.equals(artifactResult.getArtifact().getArtifactId()) &&
                    version.equals(artifactResult.getArtifact().getVersion())) {
                destinationOldFile = new File(oldFile);
                destinationNewFile = new File(newFile);
                mainArtifactFile = destinationNewFile;
            } else {
                destinationOldFile = new File("lib/" + repositoryFile.getName());
                destinationNewFile = new File("lib/" + repositoryFile.getName());
            }
            if (!destinationOldFile.exists() || destinationOldFile.lastModified() != repositoryFile.lastModified() ||
                    destinationOldFile.length() != repositoryFile.length()) {
                FileUtils.copyFile(repositoryFile, destinationNewFile);
                System.out.println("Copied from repository: " + destinationNewFile);
            }
        }
        System.out.println("Main artifact file: " + mainArtifactFile);
    }

}
